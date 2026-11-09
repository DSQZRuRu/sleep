package com.somnia.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.somnia.alarm.SmartAlarmScheduler
import com.somnia.data.repository.NightPlanner
import com.somnia.data.repository.ProfileRepository

/**
 * Programme les rappels du soir avec AlarmManager :
 *  - le rappel à l'heure du coucher (moins le décalage choisi) ;
 *  - le début de la surveillance de l'écran, 30 min avant le coucher, pendant 2 h 30.
 */
class ReminderScheduler(
    private val context: Context,
    private val prefs: ReminderPrefs,
    private val profiles: ProfileRepository,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun reschedule() {
        val s = prefs.settings.value
        val p = profiles.get()
        if (!p.onboardingDone) {
            cancel(REQ_BEDTIME, ReminderReceiver.ACTION_BEDTIME)
            cancel(REQ_WATCH_START, ReminderReceiver.ACTION_WATCH_START)
            stopWatch()
            return
        }
        val bed = ProfileRepository.idealBedtime(p)

        if (s.bedtimeEnabled) {
            setExact(NightPlanner.nextOccurrence(bed - s.offsetMin), REQ_BEDTIME, ReminderReceiver.ACTION_BEDTIME)
        } else {
            cancel(REQ_BEDTIME, ReminderReceiver.ACTION_BEDTIME)
        }

        if (s.smartEnabled) {
            val nextStart = NightPlanner.nextOccurrence(bed - WATCH_BEFORE_MIN)
            setExact(nextStart, REQ_WATCH_START, ReminderReceiver.ACTION_WATCH_START)
            // Si l'on est déjà dans la fenêtre de ce soir, la surveillance démarre tout de suite.
            val tonightStart = nextStart - DAY_MS
            val now = System.currentTimeMillis()
            if (now in tonightStart until tonightStart + WATCH_DURATION_MS && prefs.watchUntil < now) {
                startWatch(tonightStart + WATCH_DURATION_MS)
            }
        } else {
            cancel(REQ_WATCH_START, ReminderReceiver.ACTION_WATCH_START)
            stopWatch()
        }
    }

    fun startWatch(until: Long) {
        prefs.watchUntil = until
        prefs.lastPollScreenOff = false
        schedulePoll()
    }

    fun stopWatch() {
        prefs.watchUntil = 0L
        cancel(REQ_POLL, ReminderReceiver.ACTION_WATCH_POLL)
    }

    fun schedulePoll(delayMs: Long = POLL_INTERVAL_MS) =
        setExact(System.currentTimeMillis() + delayMs, REQ_POLL, ReminderReceiver.ACTION_WATCH_POLL)

    private fun pending(request: Int, action: String): PendingIntent = PendingIntent.getBroadcast(
        context, request,
        Intent(context, ReminderReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun setExact(at: Long, request: Int, action: String) {
        val pi = pending(request, action)
        if (SmartAlarmScheduler(context).canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun cancel(request: Int, action: String) = alarmManager.cancel(pending(request, action))

    companion object {
        private const val REQ_BEDTIME = 50
        private const val REQ_WATCH_START = 51
        private const val REQ_POLL = 52
        const val WATCH_BEFORE_MIN = 30
        const val WATCH_DURATION_MS = 150 * 60_000L
        const val POLL_INTERVAL_MS = 2 * 60_000L
        const val SCREEN_OFF_THRESHOLD_MS = 2 * 60_000L
        private const val DAY_MS = 24 * 3_600_000L
    }
}
