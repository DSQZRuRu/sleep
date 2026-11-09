package com.somnia.domain.game

enum class ChallengeKind { BED_BEFORE_MIDNIGHT, HEALTHY_DURATION, ON_TIME_BED, EFFICIENT, WAKE_ON_TIME, DEEP_NIGHTS }

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val kind: ChallengeKind,
    val target: Int,
    val xpReward: Int,
)

data class ChallengeProgress(val challenge: Challenge, val progress: Int, val completed: Boolean)

object ChallengeCatalog {
    val all = listOf(
        Challenge("midnight5", "Avant les douze coups", "Se coucher avant minuit 5 soirs cette semaine", ChallengeKind.BED_BEFORE_MIDNIGHT, 5, 80),
        Challenge("healthy4", "Juste ce qu'il faut", "Dormir entre 7 et 9 h pendant 4 nuits", ChallengeKind.HEALTHY_DURATION, 4, 80),
        Challenge("ontime5", "Horloge interne", "Se coucher à ±30 min de l'heure idéale 5 soirs", ChallengeKind.ON_TIME_BED, 5, 100),
        Challenge("efficient4", "Au lit pour dormir", "Atteindre 85 % d'efficacité pendant 4 nuits", ChallengeKind.EFFICIENT, 4, 70),
        Challenge("wake5", "Lever constant", "Se lever à ±30 min de l'heure prévue 5 matins", ChallengeKind.WAKE_ON_TIME, 5, 90),
        Challenge("deep3", "Plongée profonde", "3 nuits avec au moins 15 % de sommeil profond estimé", ChallengeKind.DEEP_NIGHTS, 3, 60),
    )

    /** 3 défis par semaine, choisis de façon déterministe à partir du numéro de semaine. */
    fun forWeek(weekKey: Int): List<Challenge> {
        val start = (weekKey * 7 + weekKey / 100) % all.size
        return (0 until 3).map { all[(start + it * 2) % all.size] }.distinctBy { it.id }
    }

    fun progress(challenge: Challenge, weekNights: List<NightSummary>): Int = weekNights.count {
        when (challenge.kind) {
            ChallengeKind.BED_BEFORE_MIDNIGHT -> it.beforeMidnight
            ChallengeKind.HEALTHY_DURATION -> it.healthyDuration
            ChallengeKind.ON_TIME_BED -> kotlin.math.abs(it.bedDeviation) <= 30
            ChallengeKind.EFFICIENT -> it.efficiency >= 0.85f
            ChallengeKind.WAKE_ON_TIME -> it.wokeOnTime
            ChallengeKind.DEEP_NIGHTS -> it.deepRatio >= 0.15f
        }
    }.coerceAtMost(challenge.target)
}
