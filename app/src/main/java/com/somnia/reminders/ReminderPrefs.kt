package com.somnia.reminders

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReminderSettings(
    /** Notification programmée à l'heure du coucher (moins [offsetMin]). */
    val bedtimeEnabled: Boolean = false,
    val offsetMin: Int = 15,
    /** Rappel si l'écran s'éteint vers l'heure du coucher sans suivi lancé. */
    val smartEnabled: Boolean = false,
    /** Joue les rappels sur le volume des alarmes (sonne même en mode vibreur). */
    val loud: Boolean = false,
    /** Notification du bilan, le dimanche soir. */
    val weeklyRecap: Boolean = true,
    /** Réveil doux (carillon progressif) plutôt que la sonnerie d'alarme du téléphone. */
    val gentleAlarm: Boolean = true,
    /** Envoyer les nuits vers Health Connect. */
    val healthConnect: Boolean = false,
)

/** Réglages des rappels et état de la surveillance, en SharedPreferences (lisibles depuis les receivers). */
class ReminderPrefs(context: Context) {
    private val sp = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<ReminderSettings> = _settings.asStateFlow()

    private fun read() = ReminderSettings(
        bedtimeEnabled = sp.getBoolean("bedtime", false),
        offsetMin = sp.getInt("offset", 15),
        smartEnabled = sp.getBoolean("smart", false),
        loud = sp.getBoolean("loud", false),
        weeklyRecap = sp.getBoolean("weekly", true),
        gentleAlarm = sp.getBoolean("gentle", true),
        healthConnect = sp.getBoolean("health_connect", false),
    )

    fun update(transform: (ReminderSettings) -> ReminderSettings) {
        val s = transform(_settings.value)
        sp.edit()
            .putBoolean("bedtime", s.bedtimeEnabled)
            .putInt("offset", s.offsetMin)
            .putBoolean("smart", s.smartEnabled)
            .putBoolean("loud", s.loud)
            .putBoolean("weekly", s.weeklyRecap)
            .putBoolean("gentle", s.gentleAlarm)
            .putBoolean("health_connect", s.healthConnect)
            .apply()
        _settings.value = s
    }

    /** Fin de la surveillance de l'écran pour ce soir (0 = inactive). */
    var watchUntil: Long
        get() = sp.getLong("watch_until", 0L)
        set(v) = sp.edit().putLong("watch_until", v).apply()

    /** L'écran était éteint lors de la vérification précédente (repli sans accès aux données d'utilisation). */
    var lastPollScreenOff: Boolean
        get() = sp.getBoolean("last_poll_off", false)
        set(v) = sp.edit().putBoolean("last_poll_off", v).apply()

    /** Jour (epochDay) où l'utilisateur a confirmé avoir pris la lumière du matin. */
    var morningLightDay: Long
        get() = sp.getLong("morning_light_day", -1L)
        set(v) = sp.edit().putLong("morning_light_day", v).apply()

    /** Heure du dernier rituel du soir terminé. */
    var lastRitualAt: Long
        get() = sp.getLong("last_ritual", 0L)
        set(v) = sp.edit().putLong("last_ritual", v).apply()

    fun ritualRecent(now: Long = System.currentTimeMillis()) = now - lastRitualAt in 0..2 * 3_600_000L

    /** Nuit (epochDay) pour laquelle le rappel intelligent a déjà été envoyé : un seul par nuit. */
    var lastSmartNight: Long
        get() = sp.getLong("last_smart_night", -1L)
        set(v) = sp.edit().putLong("last_smart_night", v).apply()
}
