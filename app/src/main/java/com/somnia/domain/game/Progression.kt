package com.somnia.domain.game

import com.somnia.domain.sleep.TimeMath
import java.time.LocalDate
import java.time.temporal.IsoFields

/** Résumé d'une nuit utilisé par la gamification. */
data class NightSummary(
    val sessionId: Long,
    /** Jour du soir où la nuit a commencé (epochDay). */
    val nightDay: Long,
    val bedtimeMin: Int,
    val wakeMin: Int,
    val totalSleepMin: Int,
    val efficiency: Float,
    val score: Int,
    val deepRatio: Float,
    val restless: Boolean,
    val targetBedMin: Int,
    val targetWakeMin: Int,
) {
    val bedDeviation: Int get() = TimeMath.diff(bedtimeMin, targetBedMin)
    val healthyDuration: Boolean get() = totalSleepMin in 420..540
    /** Nuit "régulière" : coucher à ±45 min de la cible et au moins 6 h de sommeil réel. */
    val regular: Boolean get() = kotlin.math.abs(bedDeviation) <= 45 && totalSleepMin >= 360
    /** Coucher avant minuit (heures du soir entre 18 h et 23 h 59). */
    val beforeMidnight: Boolean get() = bedtimeMin in (18 * 60) until TimeMath.DAY
    val wokeOnTime: Boolean get() = TimeMath.absDiff(wakeMin, targetWakeMin) <= 30
}

object WeekKey {
    fun of(epochDay: Long): Int {
        val d = LocalDate.ofEpochDay(epochDay)
        return d.get(IsoFields.WEEK_BASED_YEAR) * 100 + d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    }
    fun mondayOf(epochDay: Long): Long {
        val d = LocalDate.ofEpochDay(epochDay)
        return d.minusDays((d.dayOfWeek.value - 1).toLong()).toEpochDay()
    }
}

object XpCalculator {
    data class Line(val label: String, val xp: Int)

    /**
     * Points de la nuit. Basés sur le sommeil RÉEL (pas le temps au lit) et plafonnés à 9 h :
     * rester au lit éveillé ne rapporte rien, et l'efficacité est récompensée.
     */
    fun forNight(n: NightSummary): List<Line> {
        val lines = mutableListOf<Line>()
        val h = n.totalSleepMin
        val durationXp = when {
            h in 420..540 -> 50
            h in 360 until 420 || h in 541..600 -> 25
            h >= 180 -> 10
            else -> 0
        }
        if (durationXp > 0) lines += Line("Durée de sommeil", durationXp)
        if (n.efficiency >= 0.85f) lines += Line("Sommeil efficace", 20)
        when (kotlin.math.abs(n.bedDeviation)) {
            in 0..15 -> lines += Line("Coucher pile à l'heure", 30)
            in 16..30 -> lines += Line("Coucher à l'heure", 20)
            in 31..45 -> lines += Line("Coucher presque à l'heure", 10)
        }
        if (n.wokeOnTime) lines += Line("Lever régulier", 20)
        return lines
    }

    /** XP nécessaire pour passer du niveau [level] au suivant. */
    fun xpToNext(level: Int): Int = 100 + 60 * (level - 1)

    data class LevelInfo(val level: Int, val xpIntoLevel: Int, val xpForLevel: Int)

    fun levelFor(totalXp: Int): LevelInfo {
        var level = 1
        var remaining = totalXp
        while (remaining >= xpToNext(level)) {
            remaining -= xpToNext(level)
            level++
        }
        return LevelInfo(level, remaining, xpToNext(level))
    }
}

/** État des séries. Une protection (nuit ratée pardonnée) est disponible par semaine ISO. */
data class StreakState(
    val current: Int = 0,
    val best: Int = 0,
    val lastNightDay: Long? = null,
    val freezeUsedWeek: Int? = null,
)

data class StreakResult(val state: StreakState, val freezeUsed: Boolean, val extended: Boolean, val broken: Boolean)

object StreakManager {

    fun apply(state: StreakState, night: NightSummary): StreakResult {
        val week = WeekKey.of(night.nightDay)
        val freezeAvailable = state.freezeUsedWeek != week
        val last = state.lastNightDay

        if (last != null && night.nightDay <= last) {
            return StreakResult(state, freezeUsed = false, extended = false, broken = false)
        }

        if (!night.regular) {
            // Une nuit irrégulière peut être pardonnée une fois par semaine.
            return if (state.current > 0 && freezeAvailable) {
                StreakResult(state.copy(lastNightDay = night.nightDay, freezeUsedWeek = week), true, false, false)
            } else {
                StreakResult(state.copy(current = 0, lastNightDay = night.nightDay), false, false, state.current > 0)
            }
        }

        val gap = if (last == null) 1 else night.nightDay - last
        return when {
            gap == 1L || state.current == 0 -> {
                val c = state.current + 1
                StreakResult(state.copy(current = c, best = maxOf(state.best, c), lastNightDay = night.nightDay), false, true, false)
            }
            gap == 2L && freezeAvailable -> {
                val c = state.current + 1
                StreakResult(StreakState(c, maxOf(state.best, c), night.nightDay, week), true, true, false)
            }
            else -> StreakResult(state.copy(current = 1, best = maxOf(state.best, 1), lastNightDay = night.nightDay), false, true, true)
        }
    }
}
