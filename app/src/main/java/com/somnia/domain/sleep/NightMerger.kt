package com.somnia.domain.sleep

/**
 * Regroupe plusieurs morceaux d'une même nuit (ex. réveillé par l'alarme, puis rendormi) :
 * les morceaux sont mis bout à bout, l'intervalle entre eux compte comme éveil,
 * puis les statistiques sont recalculées sur la nuit entière.
 */
object NightMerger {
    /** Un morceau de nuit. [stages] null = pas de mesure (nuit estimée ou ajoutée à la main). */
    data class Part(val start: Long, val minutes: Int, val stages: List<SleepStage>?)

    data class Merged(val start: Long, val stages: List<SleepStage>, val analysis: NightAnalysis, val gapMinutes: Int)

    /** Écart maximal entre deux morceaux pour les considérer comme la même nuit. */
    const val MAX_GAP_MS = 3 * 3_600_000L

    fun merge(parts: List<Part>, latencyMin: Int): Merged {
        val sorted = parts.sortedBy { it.start }
        val stages = mutableListOf<SleepStage>()
        var cursor = sorted.first().start
        var gaps = 0
        for (p in sorted) {
            var own = p.stages ?: (List(latencyMin.coerceAtMost(p.minutes)) { SleepStage.AWAKE } +
                List((p.minutes - latencyMin).coerceAtLeast(0)) { SleepStage.LIGHT })
            val gap = ((p.start - cursor) / 60_000L).toInt()
            if (gap > 0) { repeat(gap) { stages += SleepStage.AWAKE }; gaps += gap }
            else if (gap < 0) own = own.drop(-gap) // chevauchement : on ne compte pas deux fois
            stages += own
            cursor = maxOf(cursor, p.start + p.minutes * 60_000L)
        }
        return Merged(sorted.first().start, stages, statsOf(stages), gaps)
    }

    /** Statistiques d'une nuit à partir de ses phases minute par minute. */
    fun statsOf(stages: List<SleepStage>): NightAnalysis {
        val onset = stages.indexOfFirst { it != SleepStage.AWAKE }.takeIf { it >= 0 }
        val finalWake = stages.indexOfLast { it != SleepStage.AWAKE }.takeIf { it >= 0 }
        var light = 0; var deep = 0
        stages.forEach { if (it == SleepStage.LIGHT) light++ else if (it == SleepStage.DEEP) deep++ }
        var awakenings = 0
        if (onset != null && finalWake != null) {
            var i: Int = onset
            while (i <= finalWake) {
                if (stages[i] == SleepStage.AWAKE) {
                    var j = i
                    while (j + 1 <= finalWake && stages[j + 1] == SleepStage.AWAKE) j++
                    if (j - i + 1 >= 2) awakenings++
                    i = j + 1
                } else i++
            }
        }
        val total = light + deep
        var cycles = 0
        for (i in stages.indices) if (stages[i] == SleepStage.DEEP && (i == 0 || stages[i - 1] != SleepStage.DEEP)) cycles++
        return NightAnalysis(
            stages = stages, sleepOnsetIndex = onset, finalWakeIndex = finalWake,
            totalSleepMin = total, lightMin = light, deepMin = deep, awakeMin = stages.size - total,
            latencyMin = onset ?: stages.size, awakenings = awakenings,
            efficiency = if (stages.isEmpty()) 0f else total.toFloat() / stages.size, detectedCycles = cycles,
        )
    }
}
