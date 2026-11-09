package com.somnia.alarm

import android.content.Context
import com.somnia.tracking.SleepTrackingService
import com.somnia.tracking.TrackingState

/** Point d'entrée unique pour faire sonner, répéter ou arrêter le réveil. */
object AlarmController {

    fun ring(context: Context) {
        var already = false
        TrackingState.update { already = it.ringing; it.copy(ringing = true) }
        if (already) return
        SmartAlarmScheduler(context).cancel()
        if (Notifications.canPost(context)) {
            Notifications.post(context, Notifications.ID_ALARM, Notifications.alarm(context))
        } else {
            AlarmSound.start(context)
        }
    }

    fun snooze(context: Context) {
        AlarmSound.stop()
        Notifications.cancel(context, Notifications.ID_ALARM)
        TrackingState.update { it.copy(ringing = false) }
        SmartAlarmScheduler(context).schedule(System.currentTimeMillis() + SmartAlarmScheduler.SNOOZE_MIN * 60_000L)
    }

    /** Arrête la sonnerie et termine la nuit (analyse + récompenses). */
    fun dismiss(context: Context) {
        AlarmSound.stop()
        Notifications.cancel(context, Notifications.ID_ALARM)
        SmartAlarmScheduler(context).cancel()
        TrackingState.update { it.copy(ringing = false) }
        if (TrackingState.status.value.running) {
            context.startService(SleepTrackingService.stopIntent(context))
        }
    }
}
