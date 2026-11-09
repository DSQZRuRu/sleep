package com.somnia.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somnia.AppContainer
import com.somnia.data.db.GameStateEntity
import com.somnia.data.repository.Clock
import com.somnia.data.repository.toSummary
import com.somnia.domain.game.BadgeCatalog
import com.somnia.domain.game.ChallengeCatalog
import com.somnia.domain.game.ChallengeProgress
import com.somnia.domain.game.CompanionLogic
import com.somnia.domain.game.CompanionMood
import com.somnia.domain.game.CompanionStage
import com.somnia.domain.game.DreamCreature
import com.somnia.domain.game.DreamCreatureCatalog
import com.somnia.domain.game.WeekKey
import com.somnia.domain.game.XpCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class CreatureEntry(val creature: DreamCreature, val count: Int)
data class BadgeEntry(val id: String, val name: String, val description: String, val symbol: String, val unlocked: Boolean)

data class JournalUi(
    val game: GameStateEntity = GameStateEntity(),
    val level: XpCalculator.LevelInfo = XpCalculator.levelFor(0),
    val stage: CompanionStage = CompanionStage.EGG,
    val mood: CompanionMood = CompanionMood.CALM,
    val freezeAvailable: Boolean = true,
    val challenges: List<ChallengeProgress> = emptyList(),
    val creatures: List<CreatureEntry> = emptyList(),
    val badges: List<BadgeEntry> = emptyList(),
) {
    val discovered get() = creatures.count { it.count > 0 }
}

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(c: AppContainer) : ViewModel() {
    private val week = WeekKey.of(Clock.today())

    val ui: StateFlow<JournalUi> = combine(
        c.gameRepository.state,
        c.gameRepository.catches,
        c.gameRepository.badges,
        c.sleepRepository.history,
        c.gameRepository.completions(week),
    ) { game, catches, badges, history, completions ->
        val counts = catches.groupingBy { it.creatureId }.eachCount()
        val unlocked = badges.map { it.badgeId }.toSet()
        val weekNights = history.map { it.toSummary() }.filter { WeekKey.of(it.nightDay) == week }
        val done = completions.map { it.challengeId }.toSet()
        JournalUi(
            game = game,
            level = XpCalculator.levelFor(game.totalXp),
            stage = CompanionStage.forCare(game.companionCare),
            mood = CompanionLogic.mood(history.take(3).map { it.score }.reversed()),
            freezeAvailable = game.freezeUsedWeek != week,
            challenges = ChallengeCatalog.forWeek(week).map {
                ChallengeProgress(it, if (it.id in done) it.target else ChallengeCatalog.progress(it, weekNights), it.id in done)
            },
            creatures = DreamCreatureCatalog.all.map { CreatureEntry(it, counts[it.id] ?: 0) },
            badges = BadgeCatalog.all.map { BadgeEntry(it.id, it.name, it.description, it.symbol, it.id in unlocked) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUi())
}
