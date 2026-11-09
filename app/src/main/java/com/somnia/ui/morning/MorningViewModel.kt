package com.somnia.ui.morning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somnia.AppContainer
import com.somnia.data.db.SleepSessionEntity
import com.somnia.data.repository.StoredRewards
import com.somnia.domain.game.CompanionLogic
import com.somnia.domain.game.CompanionMood
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MorningUi(
    val session: SleepSessionEntity? = null,
    val rewards: StoredRewards? = null,
    val mood: CompanionMood = CompanionMood.CALM,
    val rating: Int? = null,
)

class MorningViewModel(private val c: AppContainer, private val sessionId: Long) : ViewModel() {
    private val _ui = MutableStateFlow(MorningUi())
    val ui: StateFlow<MorningUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val session = c.sleepRepository.session(sessionId)
            val rewards = c.gameRepository.reward(sessionId)
            val recent = c.sleepRepository.history.first().take(3).map { it.score }.reversed()
            _ui.value = MorningUi(session, rewards, CompanionLogic.mood(recent), session?.morningRating)
        }
    }

    fun rate(value: Int) {
        _ui.value = _ui.value.copy(rating = value)
        viewModelScope.launch { c.nightProcessor.setMorningRating(sessionId, value) }
    }

    fun done() {
        viewModelScope.launch { c.sleepRepository.markRewardsSeen(sessionId) }
    }
}
