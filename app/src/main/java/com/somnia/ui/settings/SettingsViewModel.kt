package com.somnia.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somnia.AppContainer
import com.somnia.data.db.ProfileEntity
import com.somnia.reminders.ReminderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val profile: StateFlow<ProfileEntity> =
        c.profileRepository.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileEntity())

    fun update(transform: (ProfileEntity) -> ProfileEntity) {
        viewModelScope.launch { c.profileRepository.update(transform) }
    }

    val reminders: StateFlow<ReminderSettings> = c.reminderPrefs.settings

    fun updateReminders(transform: (ReminderSettings) -> ReminderSettings) = c.reminderPrefs.update(transform)

    val eventOverride: StateFlow<String?> = c.gamePrefs.eventOverride
    fun setEventOverride(id: String?) = c.gamePrefs.setEventOverride(id)
    fun addStardust() { viewModelScope.launch { c.gameRepository.addStardust(500) } }

    fun redoQuestionnaire() = update { it.copy(onboardingDone = false) }

    /** Efface toutes les données locales (nuits, profil, progression). */
    fun eraseAll() {
        viewModelScope.launch(Dispatchers.IO) { c.database.clearAllTables() }
    }
}
