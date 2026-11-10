package com.somnia.ui.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somnia.AppContainer
import com.somnia.data.repository.OwnedCreature
import com.somnia.data.repository.RoomState
import com.somnia.domain.game.CompanionLogic
import com.somnia.domain.game.CompanionMood
import com.somnia.domain.game.CompanionStage
import com.somnia.domain.game.CreatureProgression
import com.somnia.domain.game.DecorCatalog
import com.somnia.domain.game.DecorItem
import com.somnia.domain.game.DecorSlot
import com.somnia.domain.game.XpCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoomUi(
    val room: RoomState = RoomState(emptySet(), DecorCatalog.layout(emptyMap())),
    val stardust: Int = 0,
    val slotsUnlocked: Int = 1,
    val stage: CompanionStage = CompanionStage.EGG,
    val mood: CompanionMood = CompanionMood.CALM,
    val bed: List<OwnedCreature> = emptyList(),
)

class RoomViewModel(private val c: AppContainer) : ViewModel() {
    val ui: StateFlow<RoomUi> = combine(
        c.gameRepository.room, c.gameRepository.state, c.gameRepository.owned, c.sleepRepository.history,
    ) { room, game, owned, history ->
        RoomUi(
            room = room,
            stardust = game.stardust,
            slotsUnlocked = CreatureProgression.bedSlots(XpCalculator.levelFor(game.totalXp).level),
            stage = CompanionStage.forCare(game.companionCare),
            mood = CompanionLogic.mood(history.take(3).map { it.score }.reversed()),
            bed = owned.filter { it.slot != null },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoomUi())

    private val _slot = MutableStateFlow(DecorSlot.QUILT)
    val slot: StateFlow<DecorSlot> = _slot.asStateFlow()
    private val _preview = MutableStateFlow<DecorItem?>(null)
    val preview: StateFlow<DecorItem?> = _preview.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun selectSlot(s: DecorSlot) { _slot.value = s; _preview.value = null; _message.value = null }
    fun preview(item: DecorItem) { _preview.value = item; _message.value = null }

    fun place(item: DecorItem) {
        viewModelScope.launch {
            val error = c.gameRepository.buyAndPlace(item.id)
            _message.value = error ?: if (item.isDefault || item.id in ui.value.room.owned) "${item.name} installé ✓" else "${item.name} acheté et installé ✦"
            if (error == null) _preview.value = null
        }
    }
}
