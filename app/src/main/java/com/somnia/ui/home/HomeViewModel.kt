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
    val debt: com.somnia.domain.sleep.SleepDebt.Result? = null,
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
            debt = history.take(7).takeIf { it.size >= 3 }?.let { nights ->
                com.somnia.domain.sleep.SleepDebt.compute(
                    nights.map { it.totalSleepMin },
                    (profile.preferredCycles * profile.cycleMin).coerceIn(420, 540),
                )
            },
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

    private val _lightDone = MutableStateFlow(c.reminderPrefs.morningLightDay == Clock.today())
    val morningLightDone: StateFlow<Boolean> = _lightDone

    /** Défi lumière du matin : +15 ✦ une fois par jour. */
    fun confirmMorningLight() {
        if (_lightDone.value) return
        c.reminderPrefs.morningLightDay = Clock.today()
        _lightDone.value = true
        viewModelScope.launch { c.gameRepository.addStardust(15) }
    }

    fun setAlarmTonight(v: Boolean) { alarmTonight.value = v }

    fun setWakeTime(minutes: Int) {
        viewModelScope.launch { c.profileRepository.update { it.copy(targetWakeMin = minutes) } }
    }

    /** Prochaine occurrence de l'heure de lever cible. */
    fun nextAlarmAt(): Long? =
        if (ui.value.alarmTonight) com.somnia.data.repository.NightPlanner.nextWakeAt(ui.value.profile) else null

    /** Détection de secours : propose une estimation si la nuit dernière n'a pas été suivie. */
    fun refreshUsageEstimate() { viewModelScope.launch {
        _usageAccess.value = c.usageDetector.hasAccess()
        val hour = LocalTime.now(Clock.zone()).hour
        if (hour !in 5..19) { usageEstimate.value = null; return@launch }
        val estimate = withContext(Dispatchers.IO) { c.usageDetector.estimateLastNight() }
        // Déjà une nuit enregistrée sur cette période ? On ne propose rien.
        val existing = estimate?.let { c.database.sleepDao().sessionStartedBetween(it.sleepStart - 3 * 3_600_000L, it.wakeTime) }
        usageEstimate.value = if (existing != null) null else estimate
    } }

    private val _usageAccess = MutableStateFlow(true)
    val usageAccess: StateFlow<Boolean> = _usageAccess

    fun acceptUsageEstimate() {
        val e = usageEstimate.value ?: return
        usageEstimate.value = null
        viewModelScope.launch {
            val p = c.profileRepository.get()
            c.nightProcessor.createUsageSession(e.sleepStart, e.wakeTime, ProfileRepository.idealBedtime(p), p.targetWakeMin)
        }
    }
}
