package com.somnia.domain.sleep

enum class DayPhase(val label: String) {
    DAWN("Aube"), DAY("Jour"), DUSK("Crépuscule"), NIGHT("Nuit")
}

/**
 * Ambiance de la chambre calée sur VOTRE rythme (heure de lever et heure de coucher idéale),
 * pas sur l'horloge : aube autour du lever, jour, crépuscule dans les 3 h avant le coucher, nuit.
 * Les passages se font en fondu ([FADE_MIN] minutes) pour que le ciel glisse d'une phase à l'autre.
 */
object DayCycle {
    const val FADE_MIN = 40
    private const val DAWN_BEFORE_WAKE = 30
    private const val DAY_AFTER_WAKE = 60
    private const val DUSK_BEFORE_BED = 180

    private data class Boundary(val at: Int, val to: DayPhase)

    private fun boundaries(wakeMin: Int, bedMin: Int): List<Boundary> = listOf(
        Boundary(TimeMath.normalize(wakeMin - DAWN_BEFORE_WAKE), DayPhase.DAWN),
        Boundary(TimeMath.normalize(wakeMin + DAY_AFTER_WAKE), DayPhase.DAY),
        Boundary(TimeMath.normalize(bedMin - DUSK_BEFORE_BED), DayPhase.DUSK),
        Boundary(TimeMath.normalize(bedMin), DayPhase.NIGHT),
    ).sortedBy { it.at }

    /** Phase dominante à [nowMin] (minutes depuis minuit). */
    fun phase(nowMin: Int, wakeMin: Int, bedMin: Int): DayPhase {
        val b = boundaries(wakeMin, bedMin)
        val now = TimeMath.normalize(nowMin)
        return (b.lastOrNull { it.at <= now } ?: b.last()).to
    }

    /** Poids de chaque phase (somme = 1), avec fondu autour de chaque changement. */
    fun weights(nowMin: Int, wakeMin: Int, bedMin: Int): Map<DayPhase, Float> {
        val b = boundaries(wakeMin, bedMin)
        val now = TimeMath.normalize(nowMin)
        val current = phase(now, wakeMin, bedMin)
        val result = DayPhase.entries.associateWith { 0f }.toMutableMap()
        // Changement le plus proche (avant ou après) dans la fenêtre de fondu.
        for (i in b.indices) {
            val boundary = b[i]
            val previous = b[(i - 1 + b.size) % b.size].to
            val d = TimeMath.diff(now, boundary.at) // minutes depuis le changement (négatif = avant)
            if (d in -FADE_MIN / 2 until FADE_MIN / 2) {
                val t = (d + FADE_MIN / 2f) / FADE_MIN
                result[previous] = 1f - t
                result[boundary.to] = t
                return result
            }
        }
        result[current] = 1f
        return result
    }
}
