package com.somnia.tracking

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.somnia.SomniaApp
import com.somnia.alarm.AlarmController
import com.somnia.alarm.Notifications
import com.somnia.alarm.SmartAlarmScheduler
import com.somnia.data.db.EpochEntity
import com.somnia.data.repository.NightPlanner
import com.somnia.data.repository.ProfileRepository
import com.somnia.domain.sleep.EpochInput
import com.somnia.domain.sleep.SleepStageClassifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Service de premier plan qui suit la nuit, écran éteint.
 * Toutes les minutes : agrège le mouvement (et le son si activé), enregistre une « époque »
 * dans Room, et décide du réveil intelligent dans la fenêtre précédant l'heure limite.
 */
class SleepTrackingService : LifecycleService() {

    private val container by lazy { (application as SomniaApp).container }
    private lateinit var motion: MotionSensorMonitor
    private lateinit var sound: SoundLevelMonitor
    private var wakeLock: PowerManager.WakeLock? = null
    private var tickerJob: Job? = null

    private var sessionId: Long? = null
    private var alarmAt: Long? = null
    private var smartWindowMs: Long = 30 * 60_000L
    private var smartEnabled = true
    @Volatile private var phoneUsedThisMinute = false
    private val motionHistory = ArrayList<Float>()
    private var stopping = false
    private var started = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            phoneUsedThisMinute = true
        }
    }

    override fun onCreate() {
        super.onCreate()
        motion = MotionSensorMonitor(this)
        sound = SoundLevelMonitor(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> stopTracking()
            ACTION_START, null -> {
                val requestedAlarm = intent?.getLongExtra(EXTRA_ALARM_AT, -1L)?.takeIf { it > 0 }
                val defaultAlarm = intent?.getBooleanExtra(EXTRA_DEFAULT_ALARM, false) ?: false
                startTracking(requestedAlarm, resume = intent == null, defaultAlarm = defaultAlarm)
            }
        }
        return START_STICKY
    }

    private fun startTracking(requestedAlarmArg: Long?, resume: Boolean, defaultAlarm: Boolean = false) {
        var requestedAlarm = requestedAlarmArg
        if (started) return
        started = true
        // Notification immédiate (exigence des services de premier plan), affinée ensuite.
        if (!enterForeground(requestedAlarm, micWanted = false)) { stopSelf(); return }

        lifecycleScope.launch {
            val profile = container.profileRepository.get()
            // Lancement depuis un rappel : réveil à l'heure de lever du profil.
            if (defaultAlarm && requestedAlarm == null) requestedAlarm = NightPlanner.nextWakeAt(profile)
            Notifications.cancel(this@SleepTrackingService, Notifications.ID_REMINDER)
            val open = container.sleepRepository.openSession()
            if (resume && open == null) { stopSelf(); return@launch }

            val micWanted = profile.micEnabled && sound.hasPermission()
            val id = open?.id ?: container.sleepRepository.startSession(
                start = System.currentTimeMillis(),
                micEnabled = micWanted,
                alarmTarget = requestedAlarm,
                targetBed = ProfileRepository.idealBedtime(profile),
                targetWake = profile.targetWakeMin,
                ritualDone = container.reminderPrefs.ritualRecent(),
            )
            sessionId = id
            alarmAt = open?.alarmTargetTime ?: requestedAlarm
            smartEnabled = profile.smartAlarmEnabled
            smartWindowMs = profile.smartWindowMin * 60_000L

            val micActive = micWanted && enterForeground(alarmAt, micWanted = true) && sound.start()
            if (!micActive) enterForeground(alarmAt, micWanted = false)
            motion.start()
            registerScreenReceiver()
            acquireWakeLock()
            alarmAt?.let { if (it > System.currentTimeMillis()) container.alarmScheduler.schedule(it) }

            TrackingState.set(
                TrackingStatus(
                    running = true, sessionId = id, startedAt = open?.startTime ?: System.currentTimeMillis(),
                    alarmAt = alarmAt, micActive = micActive,
                )
            )
            tickerJob = launch { tickLoop(id, micActive) }
        }
    }

    private suspend fun tickLoop(id: Long, micActive: Boolean) {
        while (lifecycleScope.isActive) {
            val now = System.currentTimeMillis()
            delay(60_000L - (now % 60_000L))
            recordEpoch(id, micActive)
            checkSmartAlarm(id)
        }
    }

    private suspend fun recordEpoch(id: Long, micActive: Boolean) {
        val m = motion.drain()
        // Pendant un son d'endormissement, le micro entendrait le haut-parleur : on ignore ses pics.
        val s = if (micActive) sound.drain().let { if (com.somnia.sound.Soundscape.isPlaying) it.copy(loudEvents = 0, snoreLike = false) else it } else null
        val used = phoneUsedThisMinute
        phoneUsedThisMinute = false
        motionHistory += m
        container.sleepRepository.addEpoch(
            EpochEntity(
                sessionId = id,
                timestamp = System.currentTimeMillis() - 60_000L,
                motionCount = m,
                noiseDb = s?.avgDb ?: 0f,
                loudEvents = s?.loudEvents ?: 0,
                snoreLike = s?.snoreLike ?: false,
                phoneUsed = used,
            )
        )
    }

    private suspend fun checkSmartAlarm(id: Long) {
        val target = alarmAt ?: return
        val status = TrackingState.status.value
        if (status.ringing) return
        val now = System.currentTimeMillis()
        if (now >= target) {
            ring(id)
            return
        }
        if (!smartEnabled || now < target - smartWindowMs) return
        val recent = container.sleepRepository.lastEpochs(id, 3)
            .map { EpochInput(it.timestamp, it.motionCount, it.loudEvents, it.phoneUsed) }
        val floor = SleepStageClassifier.percentile(motionHistory, 0.10f).coerceAtMost(SleepStageClassifier.MAX_NOISE_FLOOR)
        if (SleepStageClassifier.isLightOrAwakeNow(recent, floor)) ring(id)
    }

    private suspend fun ring(id: Long) {
        container.sleepRepository.markAlarmRang(id, System.currentTimeMillis())
        alarmAt = null
        AlarmController.ring(this)
    }

    private fun stopTracking() {
        if (stopping) return
        stopping = true
        TrackingState.update { it.copy(finishing = true) }
        lifecycleScope.launch {
            withContext(NonCancellable) {
                // Après un redémarrage du processus, la session ouverte est retrouvée dans Room.
                val id = sessionId ?: container.sleepRepository.openSession()?.id
                tickerJob?.cancel()
                com.somnia.sound.Soundscape.stop()
                motion.stop()
                sound.stop()
                runCatching { unregisterReceiver(screenReceiver) }
                container.alarmScheduler.cancel()
                val processed = id?.let { container.nightProcessor.finalizeSensorSession(it, System.currentTimeMillis()) }
                releaseWakeLock()
                TrackingState.set(TrackingStatus())
                if (processed != null) {
                    Notifications.post(this@SleepTrackingService, Notifications.ID_NIGHT_READY, Notifications.nightReady(this@SleepTrackingService))
                    container.backupManager.autoBackup(force = true)
                }
            }
            ServiceCompat.stopForeground(this@SleepTrackingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun enterForeground(alarm: Long?, micWanted: Boolean): Boolean {
        val type = foregroundType(micWanted)
        return try {
            ServiceCompat.startForeground(this, Notifications.ID_TRACKING, Notifications.tracking(this, alarm, micWanted), type)
            true
        } catch (e: Exception) {
            // Par ex. démarrage micro refusé en arrière-plan : on retente sans micro.
            if (micWanted) false else runCatching {
                ServiceCompat.startForeground(this, Notifications.ID_TRACKING, Notifications.tracking(this, alarm, false), foregroundType(false))
            }.isSuccess
        }
    }

    private fun foregroundType(mic: Boolean): Int {
        var type = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        if (mic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        return type
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Somnia:tracking").apply {
            setReferenceCounted(false)
            acquire(MAX_NIGHT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        motion.stop()
        sound.stop()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.somnia.tracking.START"
        const val ACTION_STOP = "com.somnia.tracking.STOP"
        const val EXTRA_ALARM_AT = "alarm_at"
        const val EXTRA_DEFAULT_ALARM = "default_alarm"
        private const val MAX_NIGHT_MS = 14 * 3_600_000L

        fun start(context: Context, alarmAt: Long?) {
            val intent = Intent(context, SleepTrackingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_ALARM_AT, alarmAt ?: -1L)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Intent de démarrage avec le réveil par défaut du profil (utilisé par les rappels). */
        fun startWithDefaultAlarmIntent(context: Context): Intent =
            Intent(context, SleepTrackingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_DEFAULT_ALARM, true)

        fun stopIntent(context: Context): Intent =
            Intent(context, SleepTrackingService::class.java).setAction(ACTION_STOP)
    }
}
