package com.somnia.data.repository

import com.somnia.data.db.GameDao
import com.somnia.data.db.GameStateEntity
import com.somnia.data.db.NightRewardEntity
import com.somnia.domain.game.Badge
import com.somnia.domain.game.BadgeCatalog
import com.somnia.domain.game.Challenge
import com.somnia.domain.game.ChallengeCatalog
import com.somnia.domain.game.CompanionStage
import com.somnia.domain.game.DreamCreature
import com.somnia.domain.game.DreamCreatureCatalog
import com.somnia.domain.game.XpCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Récompenses d'une nuit, décodées pour l'écran du matin. */
data class StoredRewards(
    val xpLines: List<XpCalculator.Line>,
    val creatures: List<DreamCreature>,
    val newCreatureIds: Set<String>,
    val badges: List<Badge>,
    val challenges: List<Challenge>,
    val streakAfter: Int,
    val freezeUsed: Boolean,
    val streakBroken: Boolean,
    val levelBefore: Int,
    val levelAfter: Int,
    val stageBefore: CompanionStage,
    val stageAfter: CompanionStage,
    val totalXpAfter: Int,
) {
    val xpTotal get() = xpLines.sumOf { it.xp }
}

class GameRepository(private val dao: GameDao) {

    val state: Flow<GameStateEntity> = dao.observeState().map { it ?: GameStateEntity() }
    val catches = dao.observeCatches()
    val badges = dao.observeBadges()
    fun completions(week: Int) = dao.observeCompletions(week)

    suspend fun reward(sessionId: Long): StoredRewards? = dao.reward(sessionId)?.decode()

    private fun NightRewardEntity.decode(): StoredRewards {
        fun ids(s: String) = s.split(",").filter { it.isNotBlank() }
        return StoredRewards(
            xpLines = xpLines.split(";").filter { it.contains("|") }.map {
                val (label, xp) = it.split("|", limit = 2)
                XpCalculator.Line(label, xp.toIntOrNull() ?: 0)
            },
            creatures = ids(creatureIds).mapNotNull { DreamCreatureCatalog.byId(it) },
            newCreatureIds = ids(newCreatureIds).toSet(),
            badges = ids(badgeIds).mapNotNull { BadgeCatalog.byId(it) },
            challenges = ids(challengeIds).mapNotNull { id -> ChallengeCatalog.all.firstOrNull { it.id == id } },
            streakAfter = streakAfter,
            freezeUsed = freezeUsed,
            streakBroken = streakBroken,
            levelBefore = levelBefore,
            levelAfter = levelAfter,
            stageBefore = runCatching { CompanionStage.valueOf(stageBefore) }.getOrDefault(CompanionStage.EGG),
            stageAfter = runCatching { CompanionStage.valueOf(stageAfter) }.getOrDefault(CompanionStage.EGG),
            totalXpAfter = totalXpAfter,
        )
    }
}
