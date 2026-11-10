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
import kotlinx.coroutines.flow.first
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

    // ---------- Outils de test ----------
    private val _testMode = kotlinx.coroutines.flow.MutableStateFlow(c.backupManager.testModeActive)
    val testMode: StateFlow<Boolean> = _testMode
    private val _devMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val devToolsMessage: StateFlow<String?> = _devMessage

    fun startTestMode() {
        viewModelScope.launch {
            c.backupManager.startTestMode()
            _testMode.value = true
            _devMessage.value = "Mode test activé : tout ce que vous faites pourra être annulé."
        }
    }

    /** Restaure l'état d'avant le test ; l'app redémarre. */
    fun endTestMode() {
        viewModelScope.launch { _devMessage.value = c.backupManager.endTestMode() }
    }

    fun keepTestChanges() {
        c.backupManager.discardTestMode()
        _testMode.value = false
        _devMessage.value = "Mode test quitté en gardant les changements."
    }

    suspend fun stardustFromNights(): Int = c.gameRepository.stardustFromNights()

    fun resetPastTests() {
        viewModelScope.launch {
            c.gameRepository.resetTestProgress()
            _devMessage.value = "Créatures au niveau 1, décor d'origine, poussière recalculée ✓"
        }
    }

    fun simulateNight() {
        viewModelScope.launch {
            _devMessage.value = if (c.nightProcessor.simulateNight() != null) "Nuit simulée : l'écran du matin va s'ouvrir." else "Échec de la simulation."
        }
    }

    // ---------- Export pour un médecin ----------
    fun exportCsv(uri: android.net.Uri) = export(uri) { nights, out -> com.somnia.data.export.SleepExport.csv(nights, out) }
    fun exportPdf(uri: android.net.Uri) = export(uri) { nights, out -> com.somnia.data.export.SleepExport.pdf(nights, out) }

    private fun export(uri: android.net.Uri, write: (List<com.somnia.data.db.SleepSessionEntity>, java.io.OutputStream) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _backupMessage.value = runCatching {
                val nights = c.sleepRepository.history.first()
                c.appContext.contentResolver.openOutputStream(uri, "wt")!!.use { write(nights, it) }
                "Export terminé ✓ (${nights.size} nuits)"
            }.getOrElse { "Échec de l'export : ${it.message}" }
        }
    }

    // ---------- Health Connect ----------
    val healthConnectAvailable: Boolean get() = com.somnia.data.health.HealthConnectSync.available(c.appContext)

    fun onHealthPermissionResult(granted: Set<String>) {
        val ok = granted.containsAll(com.somnia.data.health.HealthConnectSync.PERMISSIONS)
        c.reminderPrefs.update { it.copy(healthConnect = ok) }
        _backupMessage.value = if (ok) "Health Connect activé : vos prochaines nuits y seront envoyées ✓" else "Autorisation Health Connect refusée."
    }

    fun redoQuestionnaire() = update { it.copy(onboardingDone = false) }

    /** Efface toutes les données locales (nuits, profil, progression). */
    fun eraseAll() {
        viewModelScope.launch(Dispatchers.IO) { c.database.clearAllTables() }
    }
}
