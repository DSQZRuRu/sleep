package com.somnia.domain.sleep

/**
 * Estimation d'une nuit non suivie à partir des instants d'utilisation du téléphone :
 * la plus longue période sans usage, commencée entre 19 h et midi (couchers tardifs compris),
 * d'au moins 3 h et d'au plus 16 h.
 */
object UsageNightEstimator {
    data class Night(val start: Long, val end: Long) {
        val minutes: Int get() = ((end - start) / 60_000L).toInt()
    }

    /**
     * @param interactions instants d'utilisation (triés ou non)
     * @param minuteOfDay convertit un instant en minutes depuis minuit (fuseau local)
     */
    fun estimate(interactions: List<Long>, minuteOfDay: (Long) -> Int): Night? {
        val t = interactions.sorted()
        var best: Night? = null
        for (i in 1 until t.size) {
            val gap = t[i] - t[i - 1]
            val startMin = minuteOfDay(t[i - 1])
            val plausibleStart = startMin >= 19 * 60 || startMin < 12 * 60
            if (gap in 3 * 3_600_000L..16 * 3_600_000L && plausibleStart && (best == null || gap > best.end - best.start)) {
                best = Night(t[i - 1], t[i])
            }
        }
        return best
    }
}
