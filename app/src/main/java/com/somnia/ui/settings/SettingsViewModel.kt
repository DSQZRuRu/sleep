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

    private val _backupMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage
    val lastBackupAt: Long get() = c.backupManager.lastBackupAt

    fun backupNow() {
        viewModelScope.launch {
            _backupMessage.value = if (c.backupManager.autoBackup(force = true))
                "Sauvegardé dans Téléchargements › Somnia ✓" else "Sauvegarde impossible (Android 10 minimum) : utilisez « Exporter »."
        }
    }

    fun exportTo(uri: android.net.Uri) {
        viewModelScope.launch {
            _backupMessage.value = runCatching {
                c.appContext.contentResolver.openOutputStream(uri, "wt")!!.use { c.backupManager.export(it) }
                "Exporté ✓"
            }.getOrElse { "Échec de l'export : ${it.message}" }
        }
    }

    fun restoreFrom(uri: android.net.Uri) {
        viewModelScope.launch {
            _backupMessage.value = c.appContext.contentResolver.openInputStream(uri)?.use { c.backupManager.restore(it) }
                ?: "Impossible d'ouvrir le fichier."
        }
    }

    fun redoQuestionnaire() = update { it.copy(onboardingDone = false) }

    /** Efface toutes les données locales (nuits, profil, progression). */
    fun eraseAll() {
        viewModelScope.launch(Dispatchers.IO) { c.database.clearAllTables() }
    }
}
