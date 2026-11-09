package com.somnia.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.somnia.data.db.BadgeUnlockEntity
import com.somnia.data.db.GameDao
import com.somnia.data.db.SomniaDatabase
import com.somnia.domain.game.CreatureProgression
import com.somnia.domain.game.DreamLine
import com.somnia.domain.game.EventCalendar
import com.somnia.domain.game.SeasonalEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
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
    val dreamBase: Int,
    val dreamScore: Int,
    val dreamLines: List<DreamLine>,
    val stardustLines: List<DreamLine>,
    val stardustGained: Int,
    val duplicateStardust: Int,
    val event: SeasonalEvent?,
) {
    val xpTotal get() = xpLines.sumOf { it.xp }
}

/** Créature possédée avec sa progression. */
data class OwnedCreature(val creature: DreamCreature, val level: Int, val slot: Int?, val count: Int) {
    val bonusPct get() = CreatureProgression.bonusPct(creature, level)
    val upgradeCost get() = CreatureProgression.upgradeCost(creature, level)
    val maxed get() = level >= CreatureProgression.MAX_LEVEL
}

/** Préférences de jeu (outils développeur). */
class GamePrefs(context: Context) {
    private val sp = context.getSharedPreferences("game", Context.MODE_PRIVATE)
    private val _eventOverride = MutableStateFlow(sp.getString("event_override", null))
    val eventOverride: StateFlow<String?> = _eventOverride.asStateFlow()

    fun setEventOverride(id: String?) {
        sp.edit().putString("event_override", id).apply()
        _eventOverride.value = id
    }

    fun activeEvent(): SeasonalEvent? = EventCalendar.active(LocalDate.now(Clock.zone()), _eventOverride.value)
}

fun encodeDreamLines(lines: List<DreamLine>) =
    lines.joinToString(";") { "${it.label.replace(";", ",").replace("|", "/")}|${it.pct}|${if (it.active) 1 else 0}" }

fun decodeDreamLines(s: String): List<DreamLine> = s.split(";").filter { it.count { c -> c == '|' } == 2 }.map {
    val (label, pct, active) = it.split("|")
    DreamLine(label, pct.toIntOrNull() ?: 0, active == "1")
}

class GameRepository(private val db: SomniaDatabase) {
    private val dao: GameDao = db.gameDao()

    val state: Flow<GameStateEntity> = dao.observeState().map { it ?: GameStateEntity() }
    val catches = dao.observeCatches()
    val badges = dao.observeBadges()
    fun completions(week: Int) = dao.observeCompletions(week)

    val owned: Flow<List<OwnedCreature>> = combine(dao.observeProgress(), dao.observeCatches()) { progress, catches ->
        val counts = catches.groupingBy { it.creatureId }.eachCount()
        progress.mapNotNull { p ->
            DreamCreatureCatalog.byId(p.creatureId)?.let { OwnedCreature(it, p.level, p.slot, counts[p.creatureId] ?: 1) }
        }
    }

    /** Installe une créature dans un emplacement du lit (celle qui l'occupait retourne au carnet). */
    suspend fun equip(creatureId: String, slot: Int) = db.withTransaction {
        val state = dao.state() ?: GameStateEntity()
        if (slot >= CreatureProgression.bedSlots(XpCalculator.levelFor(state.totalXp).level)) return@withTransaction
        dao.setSlot(creatureId, null)
        dao.clearSlot(slot)
        dao.setSlot(creatureId, slot)
    }

    /** Outil développeur. */
    suspend fun addStardust(amount: Int) = db.withTransaction {
        val state = dao.state() ?: GameStateEntity()
        dao.upsertState(state.copy(stardust = state.stardust + amount))
    }

    suspend fun unequip(creatureId: String) = dao.setSlot(creatureId, null)

    /** @return vrai si la montée de niveau a eu lieu (assez de poussière, niveau max non atteint). */
    suspend fun levelUp(creatureId: String): Boolean = db.withTransaction {
        val creature = DreamCreatureCatalog.byId(creatureId) ?: return@withTransaction false
        val p = dao.progress().firstOrNull { it.creatureId == creatureId } ?: return@withTransaction false
        if (p.level >= CreatureProgression.MAX_LEVEL) return@withTransaction false
        val state = dao.state() ?: GameStateEntity()
        val cost = CreatureProgression.upgradeCost(creature, p.level)
        if (state.stardust < cost) return@withTransaction false
        dao.upsertState(state.copy(stardust = state.stardust - cost))
        dao.setLevel(creatureId, p.level + 1)
        // Badges liés aux niveaux, débloqués immédiatement.
        val newLevel = p.level + 1
        val badges = buildList {
            if (newLevel >= 5) add("level5_creature")
            if (newLevel >= 10) add("level10_creature")
        }
        dao.insertBadges(badges.map { BadgeUnlockEntity(it, System.currentTimeMillis()) })
        true
    }

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
            dreamBase = dreamBase,
            dreamScore = dreamScore,
            dreamLines = decodeDreamLines(dreamLines.substringBefore("##")),
            stardustLines = decodeDreamLines(dreamLines.substringAfter("##", "")),
            stardustGained = stardustGained,
            duplicateStardust = duplicateStardust,
            event = EventCalendar.byId(eventId),
        )
    }
}
