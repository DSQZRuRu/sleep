package com.somnia.domain.sleep

import kotlin.math.abs
import kotlin.math.roundToInt

/** Facteurs du quotidien notés le matin, pour découvrir ce qui influence VOTRE sommeil. */
enum class SleepFactor(val id: String, val label: String, val emoji: String) {
    CAFFEINE("caffeine", "Caféine après 14 h", "☕"),
    ALCOHOL("alcohol", "Alcool", "🍷"),
    SPORT("sport", "Sport", "🏃"),
    SCREENS("screens", "Écrans tard", "📱"),
    STRESS("stress", "Stress", "😣"),
    NAP("nap", "Sieste", "💤"),
    LATE_MEAL("late_meal", "Repas tardif", "🍽"),
    MORNING_LIGHT("morning_light", "Lumière du matin", "🌤");

    companion object {
        fun parse(s: String): Set<SleepFactor> = s.split(",").mapNotNull { id -> entries.firstOrNull { it.id == id } }.toSet()
        fun encode(set: Set<SleepFactor>): String = set.joinToString(",") { it.id }
    }
}

data class FactorNight(val factors: Set<SleepFactor>, val latencyMin: Int, val totalSleepMin: Int, val score: Int)

data class FactorInsight(
    val factor: SleepFactor,
    val nightsWith: Int,
    val latencyDelta: Int,
    val sleepDelta: Int,
    val scoreDelta: Int,
) {
    /** Phrase lisible résumant l'écart le plus marquant. */
    fun sentence(): String {
        val parts = mutableListOf<String>()
        if (abs(sleepDelta) >= 15) parts += "vous dormez ${abs(sleepDelta)} min de ${if (sleepDelta > 0) "plus" else "moins"}"
        if (abs(latencyDelta) >= 5) parts += "vous vous endormez ${abs(latencyDelta)} min plus ${if (latencyDelta < 0) "vite" else "lentement"}"
        if (abs(scoreDelta) >= 5) parts += "votre score est ${if (scoreDelta > 0) "meilleur" else "plus bas"} de ${abs(scoreDelta)} points"
        return "Les jours « ${factor.label.lowercase()} », " + parts.joinToString(", ").ifEmpty { "pas de différence nette" } + "."
    }

    val notable: Boolean get() = abs(sleepDelta) >= 15 || abs(latencyDelta) >= 5 || abs(scoreDelta) >= 5
}

/**
 * Compare les nuits avec et sans chaque facteur. Il faut au moins [MIN_EACH] nuits de chaque côté :
 * ce sont des tendances personnelles, pas des preuves (beaucoup d'autres choses varient).
 */
object FactorInsights {
    const val MIN_EACH = 3

    fun compute(nights: List<FactorNight>): List<FactorInsight> = SleepFactor.entries.mapNotNull { f ->
        val with = nights.filter { f in it.factors }
        val without = nights.filter { f !in it.factors }
        if (with.size < MIN_EACH || without.size < MIN_EACH) return@mapNotNull null
        FactorInsight(
            factor = f,
            nightsWith = with.size,
            latencyDelta = (with.map { it.latencyMin }.average() - without.map { it.latencyMin }.average()).roundToInt(),
            sleepDelta = (with.map { it.totalSleepMin }.average() - without.map { it.totalSleepMin }.average()).roundToInt(),
            scoreDelta = (with.map { it.score }.average() - without.map { it.score }.average()).roundToInt(),
        )
    }.sortedByDescending { abs(it.sleepDelta) + abs(it.latencyDelta) * 2 + abs(it.scoreDelta) * 2 }

    /** Le téléphone était-il vraiment sur le matelas ? Presque aucun mouvement sur une longue nuit = probablement pas. */
    fun phoneProbablyNotOnBed(motionPerMinute: List<Float>): Boolean {
        if (motionPerMinute.size < 120) return false
        val moving = motionPerMinute.count { it > 3f }
        return moving < 5
    }
}
