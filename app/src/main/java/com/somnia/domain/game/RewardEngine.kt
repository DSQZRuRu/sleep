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
    val stardust: Int = 0,
    val bestDream: Int = 0,
    val equipped: List<EquippedCreature> = emptyList(),
    val maxCreatureLevel: Int = 1,
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
    val dream: DreamScore,
    val duplicateStardust: Int,
    val event: SeasonalEvent?,
    val expedition: ExpeditionResult = ExpeditionResult(0, ExpeditionState(), emptyList(), 0),
) {
    val stardustGained: Int get() = dream.stardust + duplicateStardust + expedition.stardust
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
    fun rollCreatures(n: NightSummary, random: Random, event: SeasonalEvent? = null, fullMoon: Boolean = false): List<DreamCreature> {
        if (n.totalSleepMin < 180) return emptyList()
        val type = sleepType(n)
        var count = 1
        if (n.score >= 70) count++
        if (n.regular) count++
        val pool = DreamCreatureCatalog.ofType(type)
        val eventPool = event?.let { DreamCreatureCatalog.ofEvent(it.id) }.orEmpty()
        // Pleine lune : une bonne nuit a une chance sur quatre d'attirer Opaline.
        val moonVisitor = if (fullMoon && n.score >= 60 && random.nextInt(100) < 25) DreamCreatureCatalog.moonCreatures.random(random) else null
        return listOfNotNull(moonVisitor) + (0 until count).map {
            val rarity = rollRarity(random, bonus = n.regular)
            // Pendant un événement, une bonne nuit peut attirer une créature de saison (40 %).
            val fromEvent = eventPool.isNotEmpty() && n.score >= 50 && random.nextInt(100) < 40
            val source = if (fromEvent) eventPool else pool
            source.filter { c -> c.rarity == rarity }
                .ifEmpty { source.filter { c -> c.rarity == Rarity.COMMON } }
                .ifEmpty { source.sortedBy { c -> c.rarity.ordinal }.take(1) }
                .random(random)
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
        previousScore: Int? = null,
        event: SeasonalEvent? = null,
        fullMoon: Boolean = false,
        ritualDone: Boolean = false,
        decor: List<DecorItem> = emptyList(),
        expedition: ExpeditionState = ExpeditionState(),
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

        // Le rituel du soir compte s'il a vraiment été suivi d'une nuit de sommeil.
        if (ritualDone && night.totalSleepMin >= 180) lines += XpCalculator.Line("Rituel du soir", 10)

        val creatures = rollCreatures(night, random, event, fullMoon)
        val newIds = creatures.map { it.id }.filter { it !in snapshot.ownedCreatureIds }.toSet()
        // Les créatures déjà connues (ou en double cette nuit) se dissolvent en poussière d'étoiles.
        val seen = snapshot.ownedCreatureIds.toMutableSet()
        var duplicateDust = 0
        for (c in creatures) {
            if (!seen.add(c.id)) duplicateDust += CreatureProgression.duplicateStardust(c.rarity)
        }
        if (newIds.isNotEmpty()) lines += XpCalculator.Line("Nouvelle créature", 15 * newIds.size)

        val care = CompanionLogic.careForNight(night.score, night.regular, night.healthyDuration)
        val stageBefore = CompanionStage.forCare(snapshot.companionCare)
        val stageAfter = CompanionStage.forCare(snapshot.companionCare + care)

        val xpGain = lines.sumOf { it.xp }
        val levelBefore = XpCalculator.levelFor(snapshot.totalXp).level
        val levelAfter = XpCalculator.levelFor(snapshot.totalXp + xpGain).level

        val dream = DreamScoreCalculator.compute(
            night, previousScore, streak.state.current, snapshot.equipped, stageAfter, event, decor,
        )
        val exp = Expedition.advance(expedition, Expedition.stepsForNight(night))

        val ctx = BadgeContext(
            nightsCount = nightsCount,
            streak = streak.state.current,
            level = levelAfter,
            uniqueCreatures = (snapshot.ownedCreatureIds + newIds).count { DreamCreatureCatalog.byId(it)?.eventId == null },
            totalCreatures = DreamCreatureCatalog.all.size,
            challengesCompleted = snapshot.challengesCompletedTotal + completed.size,
            lastNight = night,
            companionStage = stageAfter,
            regularityScore = regularityScore,
            equippedCount = snapshot.equipped.size,
            maxCreatureLevel = snapshot.maxCreatureLevel,
            bestDream = maxOf(snapshot.bestDream, dream.total),
            eventCreatureIds = (snapshot.ownedCreatureIds + newIds).filter { DreamCreatureCatalog.byId(it)?.eventId != null }.toSet(),
            regionsCompleted = expedition.regionIndex + exp.completed.size,
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
            dream = dream,
            duplicateStardust = duplicateDust,
            event = event,
            expedition = exp,
        )
    }
}
