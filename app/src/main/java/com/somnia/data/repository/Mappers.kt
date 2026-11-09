package com.somnia.data.repository

import com.somnia.data.db.SessionSource
import com.somnia.data.db.SleepSessionEntity
import com.somnia.domain.game.NightSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object Clock {
    fun zone(): ZoneId = ZoneId.systemDefault()

    fun minuteOfDay(ts: Long): Int {
        val t = Instant.ofEpochMilli(ts).atZone(zone()).toLocalTime()
        return t.hour * 60 + t.minute
    }

    /** Une nuit appartient au soir où elle a commencé (coucher à 1 h du matin → la veille). */
    fun nightDay(ts: Long): Long =
        Instant.ofEpochMilli(ts - 12 * 3_600_000L).atZone(zone()).toLocalDate().toEpochDay()

    fun today(): Long = LocalDate.now(zone()).toEpochDay()

    fun startOfDayMillis(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zone()).toInstant().toEpochMilli()
}

val SleepSessionEntity.wakeTime: Long get() = finalWake ?: endTime ?: startTime
val SleepSessionEntity.hasStages: Boolean get() = source == SessionSource.SENSORS
val SleepSessionEntity.deepRatio: Float get() = if (totalSleepMin > 0) deepMin.toFloat() / totalSleepMin else 0f
val SleepSessionEntity.restless: Boolean
    get() = hasStages && (awakenings >= 4 || (totalSleepMin > 0 && awakeMin.toFloat() / (totalSleepMin + awakeMin) > 0.2f))

fun SleepSessionEntity.toSummary(): NightSummary = NightSummary(
    sessionId = id,
    nightDay = Clock.nightDay(startTime),
    bedtimeMin = Clock.minuteOfDay(startTime),
    wakeMin = Clock.minuteOfDay(wakeTime),
    totalSleepMin = totalSleepMin,
    // Les nuits estimées par l'usage du téléphone n'ont pas d'efficacité mesurée : pas de bonus.
    efficiency = if (hasStages) efficiency else 0.8f,
    score = score,
    deepRatio = deepRatio,
    restless = restless,
    targetBedMin = targetBedMin,
    targetWakeMin = targetWakeMin,
)
