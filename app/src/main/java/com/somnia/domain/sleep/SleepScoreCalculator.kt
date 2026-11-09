package com.somnia.domain.sleep

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Score de sommeil sur 100 :
 *  - Durée (40)       : temps de sommeil réel par rapport aux 7–9 h recommandées (Hirshkowitz 2015)
 *  - Efficacité (25)  : sommeil / temps passé au lit — rester au lit éveillé fait baisser le score
 *  - Profond (15)     : proportion de sommeil profond estimé
 *  - Régularité (10)  : écart à l'heure de coucher cible
 *  - Continuité (10)  : nombre de réveils
 */
object SleepScoreCalculator {

    data class Breakdown(
        val duration: Int, val efficiency: Int, val deep: Int,
        val regularity: Int, val continuity: Int,
    ) {
        val total: Int get() = (duration + efficiency + deep + regularity + continuity).coerceIn(0, 100)
    }

    fun durationPoints(totalSleepMin: Int): Int {
        val h = totalSleepMin / 60f
        val pts = when {
            h in 7f..9f -> 40f
            h < 7f -> 40f - (7f - h) * 12f
            else -> 40f - (h - 9f) * 10f
        }
        return pts.coerceIn(0f, 40f).roundToInt()
    }

    fun score(
        analysis: NightAnalysis,
        bedtimeDeviationMin: Int?,
        hasStages: Boolean = true,
    ): Breakdown {
        val duration = durationPoints(analysis.totalSleepMin)
        val efficiency = when {
            !hasStages -> 18
            analysis.efficiency >= 0.9f -> 25
            else -> (25 - (0.9f - analysis.efficiency) * 80f).coerceIn(0f, 25f).roundToInt()
        }
        val deep = if (!hasStages) 10 else {
            val r = analysis.deepRatio
            when {
                r >= 0.15f -> 15
                else -> (r / 0.15f * 15f).roundToInt()
            }
        }
        val regularity = when (val d = bedtimeDeviationMin?.let { abs(it) }) {
            null -> 6
            in 0..20 -> 10
            in 21..45 -> 7
            in 46..90 -> 4
            else -> 1
        }
        val continuity = if (!hasStages) 7 else (10 - analysis.awakenings * 2).coerceIn(0, 10)
        return Breakdown(duration, efficiency, deep, regularity, continuity)
    }

    fun label(score: Int): String = when {
        score >= 85 -> "Excellente nuit"
        score >= 70 -> "Bonne nuit"
        score >= 55 -> "Nuit correcte"
        score >= 40 -> "Nuit difficile"
        else -> "Nuit courte"
    }
}
