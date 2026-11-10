package com.somnia.domain.game

import com.somnia.domain.sleep.RegularityCalculator

data class WeeklyRecap(
    val weekKey: Int,
    val nights: Int,
    val avgSleepMin: Int,
    val avgScore: Int,
    val regularity: Int?,
    val healthyNights: Int,
    val best: NightSummary?,
    val creaturesMet: Int,
    val message: String,
)

object WeeklyRecapCalculator {
    fun compute(weekKey: Int, nights: List<NightSummary>, creaturesMet: Int): WeeklyRecap {
        val n = nights.size
        val avgSleep = if (n == 0) 0 else nights.sumOf { it.totalSleepMin } / n
        val avgScore = if (n == 0) 0 else nights.sumOf { it.score } / n
        val reg = if (n >= 2) RegularityCalculator.compute(nights.map { it.bedtimeMin }, nights.map { it.wakeMin }).score else null
        val healthy = nights.count { it.healthyDuration }
        val message = when {
            n == 0 -> "Aucune nuit suivie cette semaine. Brumi vous attend ce soir !"
            healthy >= 5 -> "Semaine exemplaire : $healthy nuits dans la zone 7–9 h. Brumi rayonne."
            reg != null && reg >= 75 -> "Vos horaires sont réguliers : c'est l'un des meilleurs leviers pour bien dormir."
            avgSleep < 390 -> "Vos nuits ont été courtes. Avancer le coucher de 20 minutes est un bon premier pas."
            else -> "Belle semaine. Visez une heure de coucher stable pour la suivante."
        }
        return WeeklyRecap(weekKey, n, avgSleep, avgScore, reg, healthy, nights.maxByOrNull { it.score }, creaturesMet, message)
    }
}
