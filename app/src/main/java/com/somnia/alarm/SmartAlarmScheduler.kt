package com.somnia.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.somnia.MainActivity

/**
 * Programme l'heure limite du réveil avec AlarmManager.setAlarmClock (exacte, même en Doze).
 * Le déclenchement anticipé en sommeil léger est décidé par le service de suivi, dans la
 * fenêtre de [windowMin] minutes qui précède cette heure limite.
 */
class SmartAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(triggerAt: Long) {
        val operation = deadlineIntent()
        if (canScheduleExact()) {
            val show = PendingIntent.getActivity(
                context, 31, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), operation)
        } else {
            // Repli : alarme inexacte autorisée en Doze (quelques minutes de décalage possibles).
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    fun cancel() = alarmManager.cancel(deadlineIntent())

    private fun deadlineIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, 30,
        Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_DEADLINE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val SNOOZE_MIN = 9
    }
}
