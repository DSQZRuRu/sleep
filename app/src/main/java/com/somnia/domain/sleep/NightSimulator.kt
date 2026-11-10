package com.somnia.domain.sleep

import kotlin.random.Random

/**
 * Génère une nuit factice réaliste (outil développeur) : endormissement, cycles de ~90 min
 * alternant sommeil léger et profond, quelques micro-réveils, puis réveil.
 */
object NightSimulator {
    fun generate(start: Long, minutes: Int = 480, seed: Long = start): List<EpochInput> {
        val rnd = Random(seed)
        val out = ArrayList<EpochInput>(minutes)
        val latency = 10 + rnd.nextInt(15)
        for (i in 0 until minutes) {
            val t = start + i * 60_000L
            val motion = when {
                i < latency -> 20f + rnd.nextFloat() * 25f
                i >= minutes - 8 -> 25f + rnd.nextFloat() * 20f
                else -> {
                    val inCycle = (i - latency) % 90
                    val deepPhase = i < minutes * 0.7 && inCycle in 20..66
                    when {
                        rnd.nextInt(140) == 0 -> 30f // micro-réveil
                        deepPhase -> if (rnd.nextInt(12) == 0) 1f else 0f
                        else -> 2f + rnd.nextFloat() * 6f
                    }
                }
            }
            out += EpochInput(t, motion)
        }
        return out
    }
}
