package com.somnia.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somnia.AppContainer
import com.somnia.data.db.GameStateEntity
import com.somnia.data.repository.Clock
import com.somnia.data.repository.OwnedCreature
import com.somnia.data.repository.toSummary
import com.somnia.domain.game.BadgeCatalog
import com.somnia.domain.game.ChallengeCatalog
import com.somnia.domain.game.ChallengeProgress
import com.somnia.domain.game.CompanionLogic
import com.somnia.domain.game.CompanionMood
import com.somnia.domain.game.CompanionStage
import com.somnia.domain.game.CreatureProgression
import com.somnia.domain.game.DreamCreature
import com.somnia.domain.game.DreamCreatureCatalog
import com.somnia.domain.game.EventCalendar
import com.somnia.domain.game.SeasonalEvent
import com.somnia.domain.game.WeekKey
import com.somnia.domain.game.XpCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CreatureEntry(val creature: DreamCreature, val owned: OwnedCreature?) {
    val count get() = owned?.count ?: 0
}
data class BadgeEntry(val id: String, val name: String, val description: String, val symbol: String, val unlocked: Boolean)

data class JournalUi(
    val game: GameStateEntity = GameStateEntity(),
    val level: XpCalculator.LevelInfo = XpCalculator.levelFor(0),
    val stage: CompanionStage = CompanionStage.EGG,
    val mood: CompanionMood = CompanionMood.CALM,
    val freezeAvailable: Boolean = true,
    val challenges: List<ChallengeProgress> = emptyList(),
    val creatures: List<CreatureEntry> = emptyList(),
    val eventCreatures: List<CreatureEntry> = emptyList(),
    val badges: List<BadgeEntry> = emptyList(),
    val bed: List<OwnedCreature?> = listOf(null, null, null),
    val slotsUnlocked: Int = 1,
    val activeEvent: SeasonalEvent? = null,
    val nextEvent: SeasonalEvent? = null,
    val daysToNextEvent: Long = 0,
) {
    val discovered get() = creatures.count { it.count > 0 }
    val owned get() = (creatures + eventCreatures).mapNotNull { it.owned }
}

class JournalViewModel(private val c: AppContainer) : ViewModel() {
    private val week = WeekKey.of(Clock.today())
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val base = combine(
        c.gameRepository.state,
        c.gameRepository.badges,
        c.sleepRepository.history,
        c.gameRepository.completions(week),
    ) { game, badges, history, completions -> Base(game, badges.map { it.badgeId }.toSet(), history, completions.map { it.challengeId }.toSet()) }

    val ui: StateFlow<JournalUi> = combine(base, c.gameRepository.owned, c.gamePrefs.eventOverride) { b, owned, override ->
        val byId = owned.associateBy { it.creature.id }
        val weekNights = b.history.map { it.toSummary() }.filter { WeekKey.of(it.nightDay) == week }
        val level = XpCalculator.levelFor(b.game.totalXp)
        val today = LocalDate.now(Clock.zone())
        val active = EventCalendar.active(today, override)
        val next = EventCalendar.next(today)
        JournalUi(
            game = b.game,
            level = level,
            stage = CompanionStage.forCare(b.game.companionCare),
            mood = CompanionLogic.mood(b.history.take(3).map { it.score }.reversed()),
            freezeAvailable = b.game.freezeUsedWeek != week,
            challenges = ChallengeCatalog.forWeek(week).map {
                val done = it.id in b.completions
                ChallengeProgress(it, if (done) it.target else ChallengeCatalog.progress(it, weekNights), done)
            },
            creatures = DreamCreatureCatalog.all.map { CreatureEntry(it, byId[it.id]) },
            eventCreatures = DreamCreatureCatalog.eventCreatures.map { CreatureEntry(it, byId[it.id]) },
            badges = BadgeCatalog.all.map { BadgeEntry(it.id, it.name, it.description, it.symbol, it.id in b.badges) },
            bed = (0 until 3).map { slot -> owned.firstOrNull { it.slot == slot } },
            slotsUnlocked = CreatureProgression.bedSlots(level.level),
            activeEvent = active,
            nextEvent = next,
            daysToNextEvent = next?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it.nextStart(today)) } ?: 0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUi())

    fun equip(id: String, slot: Int) { viewModelScope.launch { c.gameRepository.equip(id, slot) } }
    fun unequip(id: String) { viewModelScope.launch { c.gameRepository.unequip(id) } }

    fun levelUp(id: String) {
        viewModelScope.launch {
            _message.value = if (c.gameRepository.levelUp(id)) "Niveau supérieur ! ✦" else "Pas assez de poussière d'étoiles"
        }
    }

    fun clearMessage() { _message.value = null }

    private data class Base(
        val game: GameStateEntity,
        val badges: Set<String>,
        val history: List<com.somnia.data.db.SleepSessionEntity>,
        val completions: Set<String>,
    )
}
