package com.somnia.domain.sleep

/**
 * Régularité des horaires sur les dernières nuits. La régularité est associée à de meilleurs
 * résultats de santé indépendamment de la durée (Phillips et al. 2017 ; Windred et al. 2024).
 */
object RegularityCalculator {

    data class Result(val score: Int, val bedtimeSd: Int, val wakeSd: Int, val meanBed: Int?, val meanWake: Int?)

    /** @param bedtimes / wakeTimes heures en minutes depuis minuit. */
    fun compute(bedtimes: List<Int>, wakeTimes: List<Int>): Result {
        val bedSd = TimeMath.circularStdDev(bedtimes) ?: 0
        val wakeSd = TimeMath.circularStdDev(wakeTimes) ?: 0
        val score = if (bedtimes.size < 2) 0 else (100 - (bedSd + wakeSd) / 2 * 1.1).toInt().coerceIn(0, 100)
        return Result(score, bedSd, wakeSd, TimeMath.circularMean(bedtimes), TimeMath.circularMean(wakeTimes))
    }
}
