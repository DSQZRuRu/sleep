package com.somnia.domain.sleep

/**
 * Heures de coucher idéales : heure de lever − (n cycles × durée de cycle) − temps d'endormissement.
 * Les cycles durent en moyenne ~90 min (variabilité 70–120 min) ; on ne retient en recommandation
 * que les options qui donnent 7 à 9 h de sommeil (recommandation adultes, Watson et al. 2015).
 */
object BedtimeCalculator {

    data class Option(
        val cycles: Int,
        val bedtimeMin: Int,
        val sleepDurationMin: Int,
        val recommended: Boolean,
        val fitsConstraints: Boolean,
        val ideal: Boolean = false,
    )

    fun options(
        wakeMin: Int,
        cycleMin: Int,
        latencyMin: Int,
        preferredCycles: Int,
        earliestBedMin: Int?,
        latestBedMin: Int?,
    ): List<Option> {
        val raw = (4..6).map { n ->
            val sleep = n * cycleMin
            val bed = TimeMath.normalize(wakeMin - sleep - latencyMin)
            Option(
                cycles = n,
                bedtimeMin = bed,
                sleepDurationMin = sleep,
                recommended = sleep in 420..540,
                fitsConstraints = fits(bed, earliestBedMin, latestBedMin),
            )
        }
        val idealCycles = pickIdeal(raw, preferredCycles)
        return raw.map { it.copy(ideal = it.cycles == idealCycles) }.sortedBy { -it.cycles }
    }

    private fun pickIdeal(options: List<Option>, preferred: Int): Int {
        val candidates = options.filter { it.recommended && it.fitsConstraints }
        return when {
            candidates.any { it.cycles == preferred } -> preferred
            candidates.isNotEmpty() -> candidates.maxBy { it.sleepDurationMin }.cycles
            options.any { it.recommended } -> options.filter { it.recommended }.minBy { it.sleepDurationMin }.cycles
            else -> 5
        }
    }

    /** Fenêtre de coucher possible [earliest, latest], qui peut chevaucher minuit. */
    fun fits(bed: Int, earliest: Int?, latest: Int?): Boolean {
        if (earliest == null && latest == null) return true
        // Référence à 18 h pour comparer des heures du soir et de la nuit.
        fun shifted(m: Int) = TimeMath.normalize(m - 18 * 60)
        val b = shifted(bed)
        if (earliest != null && b < shifted(earliest)) return false
        if (latest != null && b > shifted(latest)) return false
        return true
    }
}
