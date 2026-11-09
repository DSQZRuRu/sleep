package com.somnia.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.somnia.AppContainer
import com.somnia.SomniaApp
import com.somnia.alarm.Notifications
import com.somnia.data.repository.Clock
import com.somnia.tracking.TrackingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Reçoit les alarmes des rappels du soir. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = (context.applicationContext as SomniaApp).container
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_BEDTIME -> onBedtime(context, container)
                    ACTION_WATCH_START -> {
                        container.reminderScheduler.startWatch(System.currentTimeMillis() + ReminderScheduler.WATCH_DURATION_MS)
                        container.reminderScheduler.reschedule()
                    }
                    ACTION_WATCH_POLL -> onPoll(context, container)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun onBedtime(context: Context, c: AppContainer) {
        if (!isTrackingOrTrackedTonight(c)) {
            val loud = c.reminderPrefs.settings.value.loud
            Notifications.post(context, Notifications.ID_REMINDER, Notifications.bedtimeReminder(context, smart = false, loud = loud))
        }
        c.reminderScheduler.reschedule()
    }

    /**
     * Vérification toutes les 2 minutes autour de l'heure du coucher :
     * si l'écran est éteint depuis au moins 2 minutes et que le suivi n'est pas lancé,
     * on envoie un rappel (une seule fois par nuit).
     */
    private suspend fun onPoll(context: Context, c: AppContainer) {
        val prefs = c.reminderPrefs
        val scheduler = c.reminderScheduler
        val now = System.currentTimeMillis()
        if (now > prefs.watchUntil || !prefs.settings.value.smartEnabled) { scheduler.stopWatch(); return }
        val night = Clock.nightDay(now)
        if (prefs.lastSmartNight == night || isTrackingOrTrackedTonight(c)) { scheduler.stopWatch(); return }

        val screenOff = !(context.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
        val offSince = c.usageDetector.screenOffSince(now)
        val fire = screenOff && if (offSince != null) {
            now - offSince >= ReminderScheduler.SCREEN_OFF_THRESHOLD_MS
        } else {
            // Sans accès aux données d'utilisation : éteint à deux vérifications consécutives.
            prefs.lastPollScreenOff
        }
        prefs.lastPollScreenOff = screenOff

        if (fire) {
            val loud = prefs.settings.value.loud
            Notifications.post(context, Notifications.ID_REMINDER, Notifications.bedtimeReminder(context, smart = true, loud = loud))
            prefs.lastSmartNight = night
            scheduler.stopWatch()
        } else {
            scheduler.schedulePoll()
        }
    }

    private suspend fun isTrackingOrTrackedTonight(c: AppContainer): Boolean {
        if (TrackingState.status.value.running) return true
        if (c.sleepRepository.openSession() != null) return true
        val now = System.currentTimeMillis()
        return c.database.sleepDao().sessionStartedBetween(now - 6 * 3_600_000L, now) != null
    }

    companion object {
        const val ACTION_BEDTIME = "com.somnia.reminders.BEDTIME"
        const val ACTION_WATCH_START = "com.somnia.reminders.WATCH_START"
        const val ACTION_WATCH_POLL = "com.somnia.reminders.WATCH_POLL"
    }
}

/** Reprogramme les rappels après un redémarrage, une mise à jour ou un changement d'heure. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> Unit
            else -> return
        }
        val pending = goAsync()
        val container = (context.applicationContext as SomniaApp).container
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { container.reminderScheduler.reschedule() } finally { pending.finish() }
        }
    }
}
