package com.somnia.domain.sleep

import kotlin.math.roundToInt

/** Paramètres du profil qui évoluent avec les nuits enregistrées. */
data class AdaptiveParams(
    val latencyMin: Int,
    val cycleMin: Int,
    val preferredCycles: Int,
    val nightsLearned: Int,
)

/**
 * Ajuste progressivement le profil :
 *  - temps d'endormissement : moyenne mobile exponentielle des latences mesurées ;
 *  - durée de cycle : estimée seulement quand la nuit est assez nette (≥ 3 cycles détectés) ;
 *  - nombre de cycles préféré : guidé par le ressenti du matin.
 */
object ProfileAdjuster {

    fun adjust(
        current: AdaptiveParams,
        analysis: NightAnalysis?,
        morningRating: Int?,
        sleptCycles: Int?,
    ): AdaptiveParams {
        var latency = current.latencyMin.toFloat()
        var cycle = current.cycleMin.toFloat()
        var preferred = current.preferredCycles
        // Pendant les premières nuits, on apprend plus vite.
        val alpha = if (current.nightsLearned < 5) 0.35f else 0.2f

        if (analysis != null && analysis.totalSleepMin >= 180) {
            val measured = analysis.latencyMin.coerceIn(3, 60)
            latency += alpha * (measured - latency)
            if (analysis.detectedCycles >= 3) {
                val est = (analysis.totalSleepMin.toFloat() / analysis.detectedCycles).coerceIn(80f, 110f)
                cycle += 0.15f * (est - cycle)
            }
        }
        if (morningRating != null && sleptCycles != null) {
            // Mauvais ressenti malgré peu de cycles → viser un cycle de plus (dans la limite des 7–9 h).
            if (morningRating <= 2 && sleptCycles <= preferred && preferred < 6) preferred += 1
            // Très bon ressenti avec moins de cycles que prévu → la préférence peut redescendre, jamais sous 5.
            if (morningRating >= 4 && sleptCycles < preferred && preferred > 5) preferred -= 1
        }
        return AdaptiveParams(
            latencyMin = latency.roundToInt().coerceIn(5, 60),
            cycleMin = cycle.roundToInt().coerceIn(80, 110),
            preferredCycles = preferred.coerceIn(5, 6),
            nightsLearned = current.nightsLearned + 1,
        )
    }
}
