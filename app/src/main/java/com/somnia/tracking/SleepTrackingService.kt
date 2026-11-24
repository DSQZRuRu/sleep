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
    private var pendingStart: Intent? = null
    private var napMode: Int? = null
    private var autoMode = false
    private var autoWakeTarget: Long? = null
    private var sleepLikeMinutes = 0
    private var interactiveStreak = 0

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
            ACTION_CANCEL -> cancelTracking()
            ACTION_START, null -> {
                if (stopping) {
                    // Une session se termine encore : on relancera une fois la fermeture finie.
                    if (intent != null) pendingStart = Intent(intent)
                    return START_STICKY
                }
                handleStart(intent)
            }
        }
        return START_STICKY
    }

    private fun handleStart(intent: Intent?) {
        val requestedAlarm = intent?.getLongExtra(EXTRA_ALARM_AT, -1L)?.takeIf { it > 0 }
        val defaultAlarm = intent?.getBooleanExtra(EXTRA_DEFAULT_ALARM, false) ?: false
        val nap = intent?.getIntExtra(EXTRA_NAP_MIN, 0)?.takeIf { it > 0 }
        val auto = intent?.getBooleanExtra(EXTRA_AUTO, false) ?: false
        startTracking(requestedAlarm, resume = intent == null, defaultAlarm = defaultAlarm || auto, napMin = nap, auto = auto)
    }

    private fun startTracking(
        requestedAlarmArg: Long?, resume: Boolean, defaultAlarm: Boolean = false, napMin: Int? = null, auto: Boolean = false,
    ) {
        if (started) {
            // Déjà en cours (ex. lancé automatiquement) : on garde la session, mais un lancement
            // manuel affiche l'écran de nuit (le découpage et la fin automatiques restent actifs).
            if (!auto && !stopping) TrackingState.update { it.copy(auto = false) }
            return
        }
        started = true
        var requestedAlarm = requestedAlarmArg
        autoMode = auto
        if (napMin != null) requestedAlarm = System.currentTimeMillis() + napMin * 60_000L
        napMode = napMin
        // Notification immédiate (exigence des services de premier plan), affinée ensuite.
        if (!enterForeground(requestedAlarm, micWanted = false)) { stopSelf(); return }

        lifecycleScope.launch {
            val profile = container.profileRepository.get()
            // Lancement depuis un rappel ou automatique : réveil à l'heure de lever du profil
            // (sauf soirée exceptionnelle : pas de réveil imposé).
            val exceptionalTonight = container.reminderPrefs.isExceptional(com.somnia.data.repository.Clock.nightDay(System.currentTimeMillis()))
            autoWakeTarget = NightPlanner.nextWakeAt(profile)
            if (defaultAlarm && requestedAlarm == null && !exceptionalTonight) requestedAlarm = autoWakeTarget
            Notifications.cancel(this@SleepTrackingService, Notifications.ID_REMINDER)
            val open = container.sleepRepository.openSession()
            if (resume && open == null) { stopSelf(); return@launch }
            if (open != null && open.autoStarted) autoMode = true

            // Android interdit de démarrer le micro en arrière-plan : pas de micro en suivi automatique.
            val micWanted = profile.micEnabled && sound.hasPermission() && !autoMode
            val id = open?.id ?: container.sleepRepository.startSession(
                start = System.currentTimeMillis(),
                micEnabled = micWanted,
                alarmTarget = requestedAlarm,
                targetBed = ProfileRepository.idealBedtime(profile),
                targetWake = profile.targetWakeMin,
                ritualDone = napMin == null && container.reminderPrefs.ritualRecent(),
                isNap = napMin != null,
                autoStarted = autoMode,
            )
            if (open != null && open.isNap) napMode = napMode ?: ((open.alarmTargetTime ?: open.startTime) - open.startTime).div(60_000L).toInt()
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
                    alarmAt = alarmAt, micActive = micActive, napMinutes = napMode, auto = autoMode,
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
            if (autoMode) checkAutoEnd()
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
        if (m <= SleepStageClassifier.MAX_NOISE_FLOOR + 3f && !used) sleepLikeMinutes++
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
        if (napMode != null) {
            // Sieste : on réveille avant que le sommeil profond ne s'installe (8 min d'immobilité totale).
            val startedAt = TrackingState.status.value.startedAt ?: now
            if (now - startedAt >= 12 * 60_000L) {
                val last = container.sleepRepository.lastEpochs(id, 8)
                if (last.size == 8 && last.all { it.motionCount <= SleepStageClassifier.STILL_THRESHOLD && !it.phoneUsed }) ring(id)
            }
            return
        }
        if (!smartEnabled || now < target - smartWindowMs) return
        val recent = container.sleepRepository.lastEpochs(id, 3)
            .map { EpochInput(it.timestamp, it.motionCount, it.loudEvents, it.phoneUsed) }
        val floor = SleepStageClassifier.percentile(motionHistory, 0.10f).coerceAtMost(SleepStageClassifier.MAX_NOISE_FLOOR)
        if (SleepStageClassifier.isLightOrAwakeNow(recent, floor)) ring(id)
    }

    /**
     * Fin automatique d'un suivi lancé tout seul : le matin (à partir de 3 h avant l'heure de lever),
     * après au moins 3 h de sommeil, dès 15 min d'utilisation continue du téléphone ;
     * et au plus tard 3 h après l'heure de lever.
     */
    private fun checkAutoEnd() {
        val now = System.currentTimeMillis()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        interactiveStreak = if (pm.isInteractive) interactiveStreak + 1 else 0
        val wake = autoWakeTarget ?: return
        when {
            now >= wake + 3 * 3_600_000L -> stopTracking()
            now >= wake - 3 * 3_600_000L && sleepLikeMinutes >= 180 && interactiveStreak >= 15 -> stopTracking()
        }
    }

    /** « Pas ce soir » : le suivi automatique est annulé et la session effacée. */
    private fun cancelTracking() {
        if (stopping) return
        stopping = true
        lifecycleScope.launch {
            withContext(NonCancellable) {
                val id = sessionId ?: container.sleepRepository.openSession()?.id
                tickerJob?.cancel()
                motion.stop()
                sound.stop()
                runCatching { unregisterReceiver(screenReceiver) }
                container.alarmScheduler.cancel()
                id?.let { container.sleepRepository.delete(it) }
                releaseWakeLock()
                TrackingState.set(TrackingStatus())
            }
            if (!restartIfRequested()) {
                ServiceCompat.stopForeground(this@SleepTrackingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    /** Un lancement est arrivé pendant la fermeture : on repart de zéro dans ce même service. */
    private fun restartIfRequested(): Boolean {
        val next = pendingStart ?: return false
        pendingStart = null
        started = false
        stopping = false
        sessionId = null
        tickerJob = null
        alarmAt = null
        napMode = null
        autoMode = false
        autoWakeTarget = null
        sleepLikeMinutes = 0
        interactiveStreak = 0
        phoneUsedThisMinute = false
        motionHistory.clear()
        handleStart(next)
        return true
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
                    val session = container.sleepRepository.session(processed)
                    if (session?.isNap == true) {
                        Notifications.post(this@SleepTrackingService, Notifications.ID_NIGHT_READY,
                            Notifications.napDone(this@SleepTrackingService, session.totalSleepMin))
                    } else {
                        Notifications.post(this@SleepTrackingService, Notifications.ID_NIGHT_READY, Notifications.nightReady(this@SleepTrackingService))
                        container.backupManager.autoBackup(force = true)
                        if (session != null && container.reminderPrefs.settings.value.healthConnect) {
                            com.somnia.data.health.HealthConnectSync.write(this@SleepTrackingService, session, container.sleepRepository.epochs(processed))
                        }
                    }
                }
            }
            if (!restartIfRequested()) {
                ServiceCompat.stopForeground(this@SleepTrackingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun enterForeground(alarm: Long?, micWanted: Boolean): Boolean {
        val type = foregroundType(micWanted)
        return try {
            ServiceCompat.startForeground(this, Notifications.ID_TRACKING, Notifications.tracking(this, alarm, micWanted, autoMode), type)
            true
        } catch (e: Exception) {
            // Par ex. démarrage micro refusé en arrière-plan : on retente sans micro.
            if (micWanted) false else runCatching {
                ServiceCompat.startForeground(this, Notifications.ID_TRACKING, Notifications.tracking(this, alarm, false, autoMode), foregroundType(false))
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
        const val EXTRA_NAP_MIN = "nap_min"
        const val EXTRA_AUTO = "auto"
        const val ACTION_CANCEL = "com.somnia.tracking.CANCEL"
        private const val MAX_NIGHT_MS = 14 * 3_600_000L

        fun start(context: Context, alarmAt: Long?) {
            val intent = Intent(context, SleepTrackingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_ALARM_AT, alarmAt ?: -1L)
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Suivi automatique (heure de coucher ou branchement). Démarrer un service de premier plan
         * depuis l'arrière-plan peut être refusé par Android : on renvoie alors false.
         */
        fun startAuto(context: Context): Boolean = runCatching {
            ContextCompat.startForegroundService(
                context, Intent(context, SleepTrackingService::class.java).setAction(ACTION_START).putExtra(EXTRA_AUTO, true),
            )
        }.isSuccess

        fun cancelIntent(context: Context): Intent =
            Intent(context, SleepTrackingService::class.java).setAction(ACTION_CANCEL)

        /** Sieste de [minutes] minutes, avec réveil avant le sommeil profond. */
        fun startNap(context: Context, minutes: Int) {
            val intent = Intent(context, SleepTrackingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_NAP_MIN, minutes)
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
