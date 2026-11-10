package com.somnia.domain.sleep

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor

/** Phase de la lune calculée localement (approximation au mois synodique moyen, ±1 jour). */
object MoonPhase {
    private const val SYNODIC_DAYS = 29.530588853
    /** Nouvelle lune de référence : 6 janvier 2000, 18 h 14 UTC. */
    private const val REFERENCE_NEW_MOON_MS = 947_182_440_000L

    /** 0 = nouvelle lune, 0,5 = pleine lune, → 1 = retour à la nouvelle lune. */
    fun fraction(epochMillis: Long): Double {
        val days = (epochMillis - REFERENCE_NEW_MOON_MS) / 86_400_000.0
        val f = days / SYNODIC_DAYS
        return f - floor(f)
    }

    /** Part éclairée du disque, de 0 à 1. */
    fun illumination(fraction: Double): Double = (1 - cos(2 * Math.PI * fraction)) / 2

    fun isFullMoon(epochMillis: Long): Boolean = abs(fraction(epochMillis) - 0.5) * SYNODIC_DAYS <= 1.5

    fun name(fraction: Double): String = when {
        fraction < 0.03 || fraction >= 0.97 -> "Nouvelle lune"
        fraction < 0.22 -> "Premier croissant"
        fraction < 0.28 -> "Premier quartier"
        fraction < 0.47 -> "Lune gibbeuse croissante"
        fraction < 0.53 -> "Pleine lune"
        fraction < 0.72 -> "Lune gibbeuse décroissante"
        fraction < 0.78 -> "Dernier quartier"
        else -> "Dernier croissant"
    }

    /** Nombre de jours avant la prochaine pleine lune (0 si c'est ce soir). */
    fun daysToFullMoon(epochMillis: Long): Int {
        val f = fraction(epochMillis)
        val d = ((0.5 - f + 1) % 1.0) * SYNODIC_DAYS
        return if (isFullMoon(epochMillis)) 0 else d.toInt()
    }
}
