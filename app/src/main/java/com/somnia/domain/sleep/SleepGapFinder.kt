package com.somnia.domain.sleep

/**
 * Détecte un sommeil commencé AVANT le lancement du suivi : une longue période sans aucune
 * utilisation du téléphone, terminée par un court réveil (déverrouillage) juste avant le lancement.
 */
object SleepGapFinder {
    /** Durée minimale d'inactivité pour considérer qu'on dormait déjà. */
    const val MIN_GAP_MS = 45 * 60_000L
    /** Les usages dans cette fenêtre avant le lancement sont le « réveil pour lancer ». */
    const val WAKE_CLUSTER_MS = 20 * 60_000L
    /** On ne remonte pas plus loin que 6 h avant le lancement. */
    const val LOOKBACK_MS = 6 * 3_600_000L

    data class Gap(val asleepFrom: Long, val wokeAt: Long) {
        val minutes: Int get() = ((wokeAt - asleepFrom) / 60_000L).toInt()
    }

    /**
     * @param interactions instants d'utilisation du téléphone (déverrouillage, écran allumé, appli au premier plan)
     * @param trackingStart lancement du suivi
     */
    fun find(interactions: List<Long>, trackingStart: Long): Gap? {
        val before = interactions.filter { it in (trackingStart - LOOKBACK_MS)..trackingStart }.sorted()
        if (before.isEmpty()) return null
        // Réveil pour lancer : premier usage du groupe qui précède immédiatement le lancement.
        var wakeIndex = before.lastIndex
        if (trackingStart - before[wakeIndex] > WAKE_CLUSTER_MS) return null
        while (wakeIndex > 0 && before[wakeIndex] - before[wakeIndex - 1] < MIN_GAP_MS && trackingStart - before[wakeIndex - 1] <= WAKE_CLUSTER_MS) {
            wakeIndex--
        }
        if (wakeIndex == 0) return null // aucun usage avant le réveil : impossible de dater l'endormissement
        val wokeAt = before[wakeIndex]
        val asleepFrom = before[wakeIndex - 1]
        return if (wokeAt - asleepFrom >= MIN_GAP_MS) Gap(asleepFrom, wokeAt) else null
    }
}
