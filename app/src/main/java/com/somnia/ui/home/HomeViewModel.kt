package com.somnia.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somnia.AppContainer
import com.somnia.data.db.GameStateEntity
import com.somnia.data.db.ProfileEntity
import com.somnia.data.db.SleepSessionEntity
import com.somnia.data.repository.Clock
import com.somnia.data.repository.ProfileRepository
import com.somnia.domain.game.CompanionLogic
import com.somnia.domain.game.CompanionMood
import com.somnia.domain.game.CompanionStage
import com.somnia.domain.game.XpCalculator
import com.somnia.domain.sleep.BedtimeCalculator
import com.somnia.domain.tips.SleepTip
import com.somnia.domain.tips.SleepTipsCatalog
import com.somnia.tracking.PhoneUsageDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

data class HomeUi(
    val profile: ProfileEntity = ProfileEntity(),
    val options: List<BedtimeCalculator.Option> = emptyList(),
    val idealBed: Int = 23 * 60,
    val level: XpCalculator.LevelInfo = XpCalculator.levelFor(0),
    val game: GameStateEntity = GameStateEntity(),
    val stage: CompanionStage = CompanionStage.EGG,
    val mood: CompanionMood = CompanionMood.CALM,
    val lastNight: SleepSessionEntity? = null,
    val tip: SleepTip = SleepTipsCatalog.all.first(),
    val alarmTonight: Boolean = true,
    val usageEstimate: PhoneUsageDetector.Estimate? = null,
)

class HomeViewModel(private val c: AppContainer) : ViewModel() {

    private val alarmTonight = MutableStateFlow(true)
    private val usageEstimate = MutableStateFlow<PhoneUsageDetector.Estimate?>(null)

    val ui: StateFlow<HomeUi> = combine(
        c.profileRepository.profile, c.gameRepository.state, c.sleepRepository.history, alarmTonight, usageEstimate,
    ) { profile, game, history, alarm, estimate ->
        val stage = CompanionStage.forCare(game.companionCare)
        HomeUi(
            profile = profile,
            options = ProfileRepository.options(profile),
            idealBed = ProfileRepository.idealBedtime(profile),
            level = XpCalculator.levelFor(game.totalXp),
            game = game,
            stage = stage,
            mood = CompanionLogic.mood(history.take(3).map { it.score }.reversed()),
            lastNight = history.firstOrNull(),
            tip = SleepTipsCatalog.tipOfTheDay(Clock.today()),
            alarmTonight = alarm,
            usageEstimate = estimate,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUi())

    val bed: StateFlow<List<com.somnia.data.repository.OwnedCreature>> = c.gameRepository.owned
        .map { owned -> owned.filter { it.slot != null }.sortedBy { it.slot } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val event: StateFlow<com.somnia.domain.game.SeasonalEvent?> = c.gamePrefs.eventOverride
        .map { com.somnia.domain.game.EventCalendar.active(LocalDate.now(Clock.zone()), it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val room: StateFlow<com.somnia.data.repository.RoomState?> = c.gameRepository.room
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setAlarmTonight(v: Boolean) { alarmTonight.value = v }

    fun setWakeTime(minutes: Int) {
        viewModelScope.launch { c.profileRepository.update { it.copy(targetWakeMin = minutes) } }
    }

    /** Prochaine occurrence de l'heure de lever cible. */
    fun nextAlarmAt(): Long? =
        if (ui.value.alarmTonight) com.somnia.data.repository.NightPlanner.nextWakeAt(ui.value.profile) else null

    /** Détection de secours : propose une estimation si la nuit dernière n'a pas été suivie. */
    fun refreshUsageEstimate() { viewModelScope.launch {
        val hour = LocalTime.now(Clock.zone()).hour
        if (hour !in 5..14) { usageEstimate.value = null; return@launch }
        val today = LocalDate.now(Clock.zone())
        val from = today.minusDays(1).atTime(18, 0).atZone(Clock.zone()).toInstant().toEpochMilli()
        val to = today.atTime(14, 0).atZone(Clock.zone()).toInstant().toEpochMilli()
        val existing = c.database.sleepDao().sessionStartedBetween(from, to)
        usageEstimate.value = if (existing != null) null else withContext(Dispatchers.IO) { c.usageDetector.estimateLastNight() }
    } }

    fun acceptUsageEstimate() {
        val e = usageEstimate.value ?: return
        usageEstimate.value = null
        viewModelScope.launch {
            val p = c.profileRepository.get()
            c.nightProcessor.createUsageSession(e.sleepStart, e.wakeTime, ProfileRepository.idealBedtime(p), p.targetWakeMin)
        }
    }
}
