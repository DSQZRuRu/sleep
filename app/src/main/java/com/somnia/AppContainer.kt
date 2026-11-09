package com.somnia

import android.content.Context
import com.somnia.alarm.SmartAlarmScheduler
import com.somnia.data.db.SomniaDatabase
import com.somnia.data.repository.GameRepository
import com.somnia.data.repository.NightProcessor
import com.somnia.data.repository.ProfileRepository
import com.somnia.data.repository.SleepRepository
import com.somnia.reminders.ReminderPrefs
import com.somnia.reminders.ReminderScheduler
import com.somnia.tracking.PhoneUsageDetector

/** Injection de dépendances manuelle : un seul graphe d'objets pour toute l'app. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val database: SomniaDatabase = SomniaDatabase.create(appContext)
    val sleepRepository = SleepRepository(database.sleepDao())
    val profileRepository = ProfileRepository(database.profileDao())
    val gameRepository = GameRepository(database.gameDao())
    val nightProcessor = NightProcessor(database)
    val alarmScheduler = SmartAlarmScheduler(appContext)
    val usageDetector = PhoneUsageDetector(appContext)
    val reminderPrefs = ReminderPrefs(appContext)
    val reminderScheduler = ReminderScheduler(appContext, reminderPrefs, profileRepository)
}
