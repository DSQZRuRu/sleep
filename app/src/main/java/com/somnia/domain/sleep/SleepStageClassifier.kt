package com.somnia.domain.sleep

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Estimation des phases à partir de l'actigraphie du téléphone posé sur le matelas.
 *
 * Inspiré des algorithmes d'actigraphie (Cole-Kripke, Sadeh) : l'activité de chaque minute
 * est lissée par une fenêtre pondérée de ±2 minutes, puis :
 *  - ÉVEIL   : activité lissée forte, ou téléphone utilisé ;
 *  - PROFOND : immobilité quasi totale sur une fenêtre de 10 minutes ;
 *  - LÉGER   : le reste.
 * Ce n'est pas un polysomnographe : les phases sont des estimations.
 */
object SleepStageClassifier {

    private val WEIGHTS = floatArrayOf(0.04f, 0.2f, 1f, 0.2f, 0.04f)
    const val AWAKE_THRESHOLD = 18f
    const val STILL_THRESHOLD = 1.5f
    const val DEEP_WINDOW = 5 // demi-fenêtre : 5 min avant / après
    private const val ONSET_RUN = 10

    fun classify(epochs: List<EpochInput>): NightAnalysis {
        if (epochs.isEmpty()) return empty()
        val n = epochs.size

        // Soustrait le bruit de fond propre à l'appareil (10e percentile, plafonné :
        // un bruit de capteur ne dépasse pas quelques échantillons par minute).
        val raw = epochs.map { it.motionCount }
        val floor = noiseFloor(epochs)
        val activity = FloatArray(n) { i ->
            val noiseBoost = if (epochs[i].loudEvents >= 3 && raw[i] - floor > STILL_THRESHOLD) 4f else 0f
            max(0f, raw[i] - floor) + noiseBoost
        }

        val smoothed = FloatArray(n) { i ->
            var s = 0f
            for (k in -2..2) {
                val j = i + k
                if (j in 0 until n) s += activity[j] * WEIGHTS[k + 2]
            }
            s
        }

        val stages = MutableList(n) { i ->
            when {
                epochs[i].phoneUsed || smoothed[i] >= AWAKE_THRESHOLD -> SleepStage.AWAKE
                isStillAround(activity, i) -> SleepStage.DEEP
                else -> SleepStage.LIGHT
            }
        }

        // Endormissement : début de la première série de ONSET_RUN minutes sans éveil.
        val onset = (0..n - ONSET_RUN).firstOrNull { start ->
            (start until start + ONSET_RUN).none { stages[it] == SleepStage.AWAKE }
        } ?: (if (n < ONSET_RUN && stages.none { it == SleepStage.AWAKE }) 0 else null)

        if (onset == null) {
            return NightAnalysis(
                stages = List(n) { SleepStage.AWAKE }, sleepOnsetIndex = null, finalWakeIndex = null,
                totalSleepMin = 0, lightMin = 0, deepMin = 0, awakeMin = n, latencyMin = n,
                awakenings = 0, efficiency = 0f, detectedCycles = 0,
            )
        }
        val finalWake = (n - 1 downTo onset).first { stages[it] != SleepStage.AWAKE }

        for (i in 0 until onset) stages[i] = SleepStage.AWAKE
        for (i in finalWake + 1 until n) stages[i] = SleepStage.AWAKE

        // Un passage en profond de moins de 10 min est requalifié en léger.
        smoothShortRuns(stages, SleepStage.DEEP, minLength = 10, replacement = SleepStage.LIGHT)
        // Une minute d'éveil isolée entre deux phases de sommeil compte comme léger.
        smoothShortRuns(stages, SleepStage.AWAKE, minLength = 2, replacement = SleepStage.LIGHT, onlyInside = onset..finalWake)

        var light = 0; var deep = 0; var awakeInside = 0
        for (i in onset..finalWake) when (stages[i]) {
            SleepStage.LIGHT -> light++
            SleepStage.DEEP -> deep++
            SleepStage.AWAKE -> awakeInside++
        }
        val awakenings = countRuns(stages, SleepStage.AWAKE, onset..finalWake)
        val cycles = countTransitionsInto(stages, SleepStage.DEEP, onset..finalWake)
        val total = light + deep

        return NightAnalysis(
            stages = stages,
            sleepOnsetIndex = onset,
            finalWakeIndex = finalWake,
            totalSleepMin = total,
            lightMin = light,
            deepMin = deep,
            awakeMin = n - total,
            latencyMin = onset,
            awakenings = awakenings,
            efficiency = total.toFloat() / n,
            detectedCycles = cycles,
        )
    }

    /** Évaluation en direct pour le réveil intelligent : vrai si l'utilisateur semble en sommeil léger ou éveillé. */
    fun isLightOrAwakeNow(recent: List<EpochInput>, nightFloor: Float): Boolean {
        if (recent.isEmpty()) return false
        val last = recent.takeLast(3)
        if (last.any { it.phoneUsed }) return true
        val moved = last.count { it.motionCount - nightFloor > STILL_THRESHOLD * 2 }
        return moved >= 1
    }

    fun noiseFloor(epochs: List<EpochInput>): Float =
        percentile(epochs.map { it.motionCount }, 0.10f).coerceAtMost(MAX_NOISE_FLOOR)

    const val MAX_NOISE_FLOOR = 4f

    private fun isStillAround(activity: FloatArray, i: Int): Boolean {
        val from = max(0, i - DEEP_WINDOW)
        val to = minOf(activity.size - 1, i + DEEP_WINDOW)
        if (to - from < DEEP_WINDOW) return false
        for (j in from..to) if (activity[j] > STILL_THRESHOLD) return false
        return true
    }

    private fun smoothShortRuns(
        stages: MutableList<SleepStage>,
        target: SleepStage,
        minLength: Int,
        replacement: SleepStage,
        onlyInside: IntRange = stages.indices,
    ) {
        var i = onlyInside.first
        while (i <= onlyInside.last) {
            if (stages[i] == target) {
                var j = i
                while (j + 1 <= onlyInside.last && stages[j + 1] == target) j++
                if (j - i + 1 < minLength) for (k in i..j) stages[k] = replacement
                i = j + 1
            } else i++
        }
    }

    private fun countRuns(stages: List<SleepStage>, target: SleepStage, range: IntRange): Int {
        var count = 0
        var prev: SleepStage? = null
        for (i in range) {
            if (stages[i] == target && prev != target) count++
            prev = stages[i]
        }
        return count
    }

    private fun countTransitionsInto(stages: List<SleepStage>, target: SleepStage, range: IntRange): Int {
        var count = 0
        for (i in range) if (stages[i] == target && (i == range.first || stages[i - 1] != target)) count++
        return count
    }

    fun percentile(values: List<Float>, p: Float): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val idx = ((sorted.size - 1) * p).roundToInt().coerceIn(0, sorted.size - 1)
        return sorted[idx]
    }

    private fun empty() = NightAnalysis(emptyList(), null, null, 0, 0, 0, 0, 0, 0, 0f, 0)
}
