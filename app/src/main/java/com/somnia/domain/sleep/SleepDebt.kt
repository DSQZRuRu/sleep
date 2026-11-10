package com.somnia.domain.sleep

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Dette de sommeil sur 7 jours : somme des manques par rapport à votre besoin, un surplus
 * ne compensant qu'à moitié (on ne « stocke » pas le sommeil). On propose d'avancer le coucher
 * d'au plus 30 min : rattraper d'un coup en restant au lit est contre-productif.
 */
object SleepDebt {
    data class Result(val debtMin: Int, val nights: Int, val suggestionMin: Int)

    fun compute(totalSleepLast7: List<Int>, needMin: Int): Result {
        var debt = 0.0
        for (tst in totalSleepLast7.take(7)) {
            val gap = needMin - tst
            debt += if (gap > 0) gap.toDouble() else gap / 2.0
            if (debt < 0) debt = 0.0
        }
        val d = debt.roundToInt()
        val suggestion = when {
            d < 60 -> 0
            else -> ((d / 7.0 / 5).roundToInt() * 5).coerceIn(10, 30)
        }
        return Result(d, totalSleepLast7.size.coerceAtMost(7), suggestion)
    }
}

/**
 * Jet lag social (Wittmann et al., 2006) : écart entre le milieu du sommeil les jours libres
 * (nuits de vendredi et samedi) et les jours de travail.
 */
object SocialJetlag {
    data class Night(val nightDayOfWeek: Int, val bedMin: Int, val wakeMin: Int)
    data class Result(val minutes: Int, val weekdayMid: Int, val weekendMid: Int)

    private fun midSleep(n: Night): Int {
        val duration = TimeMath.normalize(n.wakeMin - n.bedMin)
        return TimeMath.normalize(n.bedMin + duration / 2)
    }

    /** @param nightDayOfWeek 1 = lundi … 7 = dimanche (jour du soir où la nuit commence). */
    fun compute(nights: List<Night>): Result? {
        val free = nights.filter { it.nightDayOfWeek == 5 || it.nightDayOfWeek == 6 }
        val work = nights.filter { it.nightDayOfWeek !in listOf(5, 6) }
        if (free.isEmpty() || work.size < 2) return null
        val wMid = TimeMath.circularMean(work.map(::midSleep)) ?: return null
        val fMid = TimeMath.circularMean(free.map(::midSleep)) ?: return null
        return Result(abs(TimeMath.diff(fMid, wMid)), wMid, fMid)
    }
}
