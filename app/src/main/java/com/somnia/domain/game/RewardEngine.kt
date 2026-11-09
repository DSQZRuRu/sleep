package com.somnia.domain.game

import com.somnia.domain.sleep.SleepType
import kotlin.random.Random

data class GameSnapshot(
    val totalXp: Int,
    val streak: StreakState,
    val companionCare: Int,
    val ownedCreatureIds: Set<String>,
    val creatureCatches: Int,
    val unlockedBadges: Set<String>,
    val challengesCompletedTotal: Int,
    val completedThisWeek: Set<String>,
)

data class NightRewards(
    val xpLines: List<XpCalculator.Line>,
    val creatures: List<DreamCreature>,
    val newCreatureIds: Set<String>,
    val completedChallenges: List<Challenge>,
    val newBadges: List<Badge>,
    val streak: StreakResult,
    val levelBefore: Int,
    val levelAfter: Int,
    val careGained: Int,
    val stageBefore: CompanionStage,
    val stageAfter: CompanionStage,
    val totalXpAfter: Int,
) {
    val xpTotal: Int get() = xpLines.sumOf { it.xp }
    val leveledUp: Boolean get() = levelAfter > levelBefore
    val evolved: Boolean get() = stageAfter != stageBefore
}

object RewardEngine {

    fun sleepType(n: NightSummary): SleepType = when {
        n.restless -> SleepType.RESTLESS
        n.deepRatio >= 0.15f -> SleepType.DEEP
        else -> SleepType.LIGHT
    }

    /**
     * Créatures de la nuit. Aucune n'apparaît si le sommeil réel est < 3 h : on récompense
     * le sommeil, pas le temps passé au lit. Le nombre augmente avec la qualité et la régularité.
     */
    fun rollCreatures(n: NightSummary, random: Random): List<DreamCreature> {
        if (n.totalSleepMin < 180) return emptyList()
        val type = sleepType(n)
        var count = 1
        if (n.score >= 70) count++
        if (n.regular) count++
        val pool = DreamCreatureCatalog.ofType(type)
        return (0 until count).map {
            val rarity = rollRarity(random, bonus = n.regular)
            pool.filter { c -> c.rarity == rarity }.ifEmpty { pool.filter { c -> c.rarity == Rarity.COMMON } }.random(random)
        }
    }

    private fun rollRarity(random: Random, bonus: Boolean): Rarity {
        val weights = Rarity.entries.associateWith { if (bonus && it != Rarity.COMMON) it.weight * 3 / 2 else it.weight }
        var roll = random.nextInt(weights.values.sum())
        for ((rarity, w) in weights) {
            if (roll < w) return rarity
            roll -= w
        }
        return Rarity.COMMON
    }

    fun process(
        night: NightSummary,
        weekNights: List<NightSummary>,
        snapshot: GameSnapshot,
        nightsCount: Int,
        regularityScore: Int,
        random: Random = Random(night.sessionId * 31 + night.nightDay),
    ): NightRewards {
        val lines = XpCalculator.forNight(night).toMutableList()

        val streak = StreakManager.apply(snapshot.streak, night)
        if (streak.extended && streak.state.current >= 2) {
            lines += XpCalculator.Line("Série de ${streak.state.current} nuits", (5 * streak.state.current).coerceAtMost(50))
        }

        val week = WeekKey.of(night.nightDay)
        val completed = ChallengeCatalog.forWeek(week).filter { c ->
            c.id !in snapshot.completedThisWeek && ChallengeCatalog.progress(c, weekNights) >= c.target
        }
        completed.forEach { lines += XpCalculator.Line("Défi : ${it.title}", it.xpReward) }

        val creatures = rollCreatures(night, random)
        val newIds = creatures.map { it.id }.filter { it !in snapshot.ownedCreatureIds }.toSet()
        if (newIds.isNotEmpty()) lines += XpCalculator.Line("Nouvelle créature", 15 * newIds.size)

        val care = CompanionLogic.careForNight(night.score, night.regular, night.healthyDuration)
        val stageBefore = CompanionStage.forCare(snapshot.companionCare)
        val stageAfter = CompanionStage.forCare(snapshot.companionCare + care)

        val xpGain = lines.sumOf { it.xp }
        val levelBefore = XpCalculator.levelFor(snapshot.totalXp).level
        val levelAfter = XpCalculator.levelFor(snapshot.totalXp + xpGain).level

        val ctx = BadgeContext(
            nightsCount = nightsCount,
            streak = streak.state.current,
            level = levelAfter,
            uniqueCreatures = (snapshot.ownedCreatureIds + newIds).size,
            totalCreatures = DreamCreatureCatalog.all.size,
            challengesCompleted = snapshot.challengesCompletedTotal + completed.size,
            lastNight = night,
            companionStage = stageAfter,
            regularityScore = regularityScore,
        )
        val newBadges = BadgeCatalog.earned(ctx).filter { it !in snapshot.unlockedBadges }.mapNotNull { BadgeCatalog.byId(it) }

        return NightRewards(
            xpLines = lines,
            creatures = creatures,
            newCreatureIds = newIds,
            completedChallenges = completed,
            newBadges = newBadges,
            streak = streak,
            levelBefore = levelBefore,
            levelAfter = levelAfter,
            careGained = care,
            stageBefore = stageBefore,
            stageAfter = stageAfter,
            totalXpAfter = snapshot.totalXp + xpGain,
        )
    }
}
