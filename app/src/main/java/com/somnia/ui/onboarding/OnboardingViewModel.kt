package com.somnia.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somnia.data.db.ProfileEntity
import com.somnia.data.repository.ProfileRepository
import com.somnia.domain.sleep.BedtimeCalculator
import com.somnia.domain.sleep.Chronotype
import com.somnia.domain.sleep.ChronotypeQuestionnaire
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingState(
    val step: Int = 0,
    val wakeMin: Int = 7 * 60,
    val latencyMin: Int = 15,
    val hasEarliest: Boolean = false,
    val earliestBedMin: Int = 22 * 60,
    val hasLatest: Boolean = false,
    val latestBedMin: Int = 0,
    val weekendDifferent: Boolean = false,
    val micEnabled: Boolean = false,
    val answers: Map<Int, Int> = emptyMap(),
) {
    val chronoComplete get() = answers.size == ChronotypeQuestionnaire.questions.size
    val chronoScore get() = answers.values.sum()
    val chronotype: Chronotype get() = ChronotypeQuestionnaire.classify(chronoScore)

    fun toProfile(base: ProfileEntity) = base.copy(
        onboardingDone = true,
        targetWakeMin = wakeMin,
        latencyMin = latencyMin,
        earliestBedMin = earliestBedMin.takeIf { hasEarliest },
        latestBedMin = latestBedMin.takeIf { hasLatest },
        weekendDifferent = weekendDifferent,
        chronotypeScore = chronoScore,
        chronotype = chronotype.name,
        micEnabled = micEnabled,
    )

    fun options(): List<BedtimeCalculator.Option> = ProfileRepository.options(toProfile(ProfileEntity()))
}

class OnboardingViewModel(
    private val profiles: ProfileRepository,
    private val backup: com.somnia.data.backup.BackupManager,
    private val appContext: android.content.Context,
) : ViewModel() {
    private val _restoreError = MutableStateFlow<String?>(null)
    val restoreError: StateFlow<String?> = _restoreError.asStateFlow()
    private val _restoring = MutableStateFlow(false)
    val restoring: StateFlow<Boolean> = _restoring.asStateFlow()

    /** Restaure une sauvegarde choisie par l'utilisateur ; l'app redémarre si tout va bien. */
    fun restore(uri: android.net.Uri) {
        _restoring.value = true
        viewModelScope.launch {
            val error = appContext.contentResolver.openInputStream(uri)?.use { backup.restore(it) }
                ?: "Impossible d'ouvrir le fichier."
            _restoreError.value = error
            _restoring.value = false
        }
    }

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        // Relancer le questionnaire depuis les réglages garde les valeurs actuelles.
        viewModelScope.launch {
            val p = profiles.get()
            _state.update {
                it.copy(
                    wakeMin = p.targetWakeMin, latencyMin = p.latencyMin,
                    hasEarliest = p.earliestBedMin != null, earliestBedMin = p.earliestBedMin ?: 22 * 60,
                    hasLatest = p.latestBedMin != null, latestBedMin = p.latestBedMin ?: 0,
                    weekendDifferent = p.weekendDifferent, micEnabled = p.micEnabled,
                )
            }
        }
    }

    fun update(transform: (OnboardingState) -> OnboardingState) = _state.update(transform)
    fun answer(questionId: Int, value: Int) = _state.update { it.copy(answers = it.answers + (questionId to value)) }
    fun next() = _state.update { it.copy(step = it.step + 1) }
    fun back() = _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    fun finish() {
        viewModelScope.launch {
            val p = profiles.get()
            profiles.save(_state.value.toProfile(p))
        }
    }
}
