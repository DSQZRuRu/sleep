package com.somnia.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.somnia.MainActivity
import com.somnia.R
import com.somnia.tracking.SleepTrackingService
import java.text.DateFormat
import java.util.Date

object Notifications {
    const val CHANNEL_TRACKING = "tracking"
    const val CHANNEL_ALARM = "alarm_v1"
    const val CHANNEL_INFO = "info"
    const val CHANNEL_REMINDER = "reminder"
    const val CHANNEL_REMINDER_LOUD = "reminder_loud"

    const val ID_TRACKING = 1
    const val ID_ALARM = 2
    const val ID_NIGHT_READY = 3
    const val ID_REMINDER = 4
    const val ID_TEST = 5

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TRACKING, "Suivi de la nuit", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Notification discrète pendant que Somnia veille sur votre sommeil"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARM, "Réveil", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Sonnerie du réveil intelligent"
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 900)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_INFO, "Récompenses du matin", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, "Rappels du coucher", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Rappel pour lancer le suivi de la nuit (respecte le mode vibreur)"
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER_LOUD, "Rappels du coucher (même en vibreur)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Rappel joué sur le volume des alarmes : il sonne même en mode vibreur"
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                )
                enableVibration(true)
            }
        )
    }

    /**
     * Rappel de lancer le suivi. [smart] : déclenché parce que l'écran s'est éteint
     * vers l'heure du coucher sans suivi en cours.
     */
    fun bedtimeReminder(context: Context, smart: Boolean, loud: Boolean): Notification {
        val start = PendingIntent.getForegroundService(
            context, 40, SleepTrackingService.startWithDefaultAlarmIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, if (loud) CHANNEL_REMINDER_LOUD else CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle(if (smart) "Vous allez dormir ? 🌙" else "C'est l'heure de se coucher 🌙")
            .setContentText(
                if (smart) "L'écran est éteint mais le suivi n'est pas lancé. Brumi vous attend !"
                else "Pensez à lancer le suivi de votre nuit dans Somnia."
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .addAction(0, "Lancer le suivi", start)
            .build()
    }

    fun test(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle("Notification de test ✓")
            .setContentText("Les notifications de Somnia fonctionnent.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 10,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun tracking(context: Context, alarmAt: Long?, micActive: Boolean): Notification {
        val stop = PendingIntent.getService(
            context, 11, SleepTrackingService.stopIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmText = alarmAt?.let { "Réveil intelligent avant ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it))}" }
            ?: "Sans réveil"
        return NotificationCompat.Builder(context, CHANNEL_TRACKING)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle("Somnia veille sur votre nuit")
            .setContentText(if (micActive) "$alarmText · micro actif (rien n'est enregistré)" else alarmText)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openAppIntent(context))
            .addAction(0, "Terminer la nuit", stop)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun alarm(context: Context): Notification {
        val fullScreen = PendingIntent.getActivity(
            context, 20,
            Intent(context, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismiss = PendingIntent.getBroadcast(
            context, 21, Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_DISMISS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snooze = PendingIntent.getBroadcast(
            context, 22, Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle("Bonjour !")
            .setContentText("C'est le bon moment pour vous lever en douceur.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(0, "Je me lève", dismiss)
            .addAction(0, "Encore 9 min", snooze)
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT
        return n
    }

    fun nightReady(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_INFO)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle("Votre nuit est analysée")
            .setContentText("Brumi et les créatures du rêve vous attendent ✨")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    @Suppress("MissingPermission")
    fun post(context: Context, id: Int, notification: Notification): Boolean {
        if (!canPost(context)) return false
        return try {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)
}
