package com.somnia.data.repository

import com.somnia.data.db.ProfileEntity
import com.somnia.domain.sleep.TimeMath
import java.time.LocalTime
import java.time.ZonedDateTime

/** Calcule les prochaines occurrences des heures du profil (coucher, lever). */
object NightPlanner {

    /** Prochaine occurrence de [minuteOfDay] au moins [leadMin] minutes après maintenant. */
    fun nextOccurrence(minuteOfDay: Int, leadMin: Long = 1, now: ZonedDateTime = ZonedDateTime.now(Clock.zone())): Long {
        val m = TimeMath.normalize(minuteOfDay)
        var target = now.with(LocalTime.of(m / 60, m % 60)).withSecond(0).withNano(0)
        if (!target.isAfter(now.plusMinutes(leadMin))) target = target.plusDays(1)
        return target.toInstant().toEpochMilli()
    }

    /** Heure limite du réveil pour une nuit qui commence maintenant. */
    fun nextWakeAt(profile: ProfileEntity): Long = nextOccurrence(profile.targetWakeMin, leadMin = 10)
}
