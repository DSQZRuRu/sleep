package com.somnia.domain.sleep

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Outils pour manipuler des heures exprimées en minutes depuis minuit (0..1439). */
object TimeMath {
    const val DAY = 1440

    fun normalize(m: Int): Int = ((m % DAY) + DAY) % DAY

    /** Différence signée la plus courte entre deux heures de la journée (a - b), dans [-720, 720]. */
    fun diff(a: Int, b: Int): Int {
        var d = normalize(a) - normalize(b)
        if (d > DAY / 2) d -= DAY
        if (d < -DAY / 2) d += DAY
        return d
    }

    fun absDiff(a: Int, b: Int): Int = abs(diff(a, b))

    /** Moyenne circulaire (gère 23:30 et 00:30 → 00:00). */
    fun circularMean(values: List<Int>): Int? {
        if (values.isEmpty()) return null
        var sx = 0.0; var sy = 0.0
        values.forEach {
            val a = 2 * Math.PI * it / DAY
            sx += cos(a); sy += sin(a)
        }
        val mean = atan2(sy / values.size, sx / values.size)
        return normalize((mean / (2 * Math.PI) * DAY).roundToInt())
    }

    /** Écart-type circulaire approximé, en minutes. */
    fun circularStdDev(values: List<Int>): Int? {
        val mean = circularMean(values) ?: return null
        if (values.size < 2) return 0
        val variance = values.sumOf { val d = diff(it, mean).toDouble(); d * d } / values.size
        return sqrt(variance).roundToInt()
    }

    fun format(m: Int): String {
        val n = normalize(m)
        return "%02d:%02d".format(n / 60, n % 60)
    }

    fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h} h ${"%02d".format(m)}" else "$m min"
    }
}
