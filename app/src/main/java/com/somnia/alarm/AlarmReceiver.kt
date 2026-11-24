package com.somnia.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DEADLINE -> AlarmController.ring(context)
            ACTION_DISMISS -> AlarmController.dismiss(context)
            ACTION_SNOOZE -> AlarmController.snooze(context)
            ACTION_BACK_TO_SLEEP -> AlarmController.backToSleep(context)
        }
    }

    companion object {
        const val ACTION_DEADLINE = "com.somnia.alarm.DEADLINE"
        const val ACTION_DISMISS = "com.somnia.alarm.DISMISS"
        const val ACTION_SNOOZE = "com.somnia.alarm.SNOOZE"
        const val ACTION_BACK_TO_SLEEP = "com.somnia.alarm.BACK_TO_SLEEP"
    }
}
