package com.somnia.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somnia.AppContainer
import com.somnia.data.db.SleepSessionEntity
import com.somnia.data.repository.Clock
import com.somnia.data.repository.wakeTime
import com.somnia.domain.sleep.RegularityCalculator
import com.somnia.domain.sleep.SleepStage
import com.somnia.ui.components.BarDatum
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class HistoryUi(
    val sessions: List<SleepSessionEntity> = emptyList(),
    val week: List<BarDatum> = emptyList(),
    val regularity: RegularityCalculator.Result? = null,
    val avgSleep: Int = 0,
    val avgScore: Int = 0,
    val weekOffset: Int = 0,
)

class HistoryViewModel(private val c: AppContainer) : ViewModel() {
    private val offset = MutableStateFlow(0)

    val ui: StateFlow<HistoryUi> = kotlinx.coroutines.flow.combine(c.sleepRepository.history, offset) { sessions, off ->
        val today = Clock.today()
        val days = (6 downTo 0).map { today - it - 1 - off * 7L }
        val byNight = sessions.groupBy { Clock.nightDay(it.startTime) }
        val week = days.map { d ->
            val nights = byNight[d].orEmpty()
            BarDatum(
                label = LocalDate.ofEpochDay(d).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.FRENCH).take(3),
                sleepMin = nights.sumOf { it.totalSleepMin },
                deepMin = nights.sumOf { it.deepMin },
                score = nights.maxOfOrNull { it.score },
            )
        }
        val inWeek = sessions.filter { Clock.nightDay(it.startTime) in days }
        val reg = if (inWeek.size >= 2) RegularityCalculator.compute(
            inWeek.map { Clock.minuteOfDay(it.startTime) }, inWeek.map { Clock.minuteOfDay(it.wakeTime) },
        ) else null
        HistoryUi(
            sessions = sessions,
            week = week,
            regularity = reg,
            avgSleep = if (inWeek.isEmpty()) 0 else inWeek.sumOf { it.totalSleepMin } / inWeek.size,
            avgScore = if (inWeek.isEmpty()) 0 else inWeek.sumOf { it.score } / inWeek.size,
            weekOffset = off,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUi())

    fun previousWeek() { offset.value += 1 }
    fun nextWeek() { if (offset.value > 0) offset.value -= 1 }
}

data class NightDetailUi(
    val session: SleepSessionEntity? = null,
    val stages: List<SleepStage> = emptyList(),
    val noise: List<Float> = emptyList(),
    val rewards: com.somnia.data.repository.StoredRewards? = null,
    val phoneNotOnBed: Boolean = false,
    val deleted: Boolean = false,
)

class NightDetailViewModel(private val c: AppContainer, private val id: Long) : ViewModel() {
    private val _ui = MutableStateFlow(NightDetailUi())
    val ui: StateFlow<NightDetailUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val s = c.sleepRepository.session(id)
            val epochs = c.sleepRepository.epochs(id)
            _ui.value = NightDetailUi(
                s, epochs.map { SleepStage.fromCode(it.stage) }, epochs.map { it.noiseDb }, c.gameRepository.reward(id),
                phoneNotOnBed = com.somnia.domain.sleep.FactorInsights.phoneProbablyNotOnBed(epochs.map { it.motionCount }),
            )
        }
    }

    fun delete() {
        viewModelScope.launch {
            c.sleepRepository.delete(id)
            _ui.value = _ui.value.copy(deleted = true)
        }
    }
}
