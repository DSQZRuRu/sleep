package com.somnia.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data class UpToDate(val manual: Boolean) : UpdateUi
    data class Available(val info: UpdateInfo) : UpdateUi
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateUi
    data class Ready(val info: UpdateInfo, val file: File) : UpdateUi
    data class Error(val message: String, val info: UpdateInfo?, val manual: Boolean) : UpdateUi
}

class UpdateViewModel(private val appContext: Context) : ViewModel() {
    private val _ui = MutableStateFlow<UpdateUi>(UpdateUi.Idle)
    val ui: StateFlow<UpdateUi> = _ui.asStateFlow()
    private var dismissedVersion = -1

    init { check(manual = false) }

    /** [manual] : demandé depuis les réglages, on affiche aussi « à jour » et les erreurs. */
    fun check(manual: Boolean) {
        if (_ui.value is UpdateUi.Downloading) return
        _ui.value = UpdateUi.Checking
        viewModelScope.launch {
            _ui.value = when (val r = UpdateChecker.check()) {
                is UpdateCheck.Available ->
                    if (!manual && r.info.versionCode == dismissedVersion) UpdateUi.Idle else UpdateUi.Available(r.info)
                UpdateCheck.UpToDate -> UpdateUi.UpToDate(manual)
                is UpdateCheck.Failed -> UpdateUi.Error(r.reason, null, manual)
            }
        }
    }

    fun download(info: UpdateInfo) {
        _ui.value = UpdateUi.Downloading(info, 0f)
        viewModelScope.launch {
            _ui.value = try {
                val file = UpdateInstaller.download(appContext, info) { p -> _ui.value = UpdateUi.Downloading(info, p) }
                UpdateUi.Ready(info, file)
            } catch (e: Exception) {
                UpdateUi.Error(e.message ?: "Échec du téléchargement", info, true)
            }
        }
    }

    fun dismiss() {
        (_ui.value as? UpdateUi.Available)?.let { dismissedVersion = it.info.versionCode }
        _ui.value = UpdateUi.Idle
    }
}
