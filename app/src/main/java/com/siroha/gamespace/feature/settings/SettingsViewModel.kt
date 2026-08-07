package com.siroha.gamespace.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siroha.gamespace.data.backup.BackupRepository
import com.siroha.gamespace.data.settings.AppSettings
import com.siroha.gamespace.data.settings.DarkModePreference
import com.siroha.gamespace.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    fun setDarkMode(mode: DarkModePreference) {
        viewModelScope.launch { settingsRepository.setDarkMode(mode) }
    }

    fun setAmoledEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAmoledEnabled(enabled) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColorEnabled(enabled) }
    }

    fun exportLibrary(destination: Uri) {
        viewModelScope.launch {
            val success = backupRepository.exportLibrary(destination)
            _backupMessage.value = if (success) "Library berhasil diekspor" else "Ekspor gagal"
        }
    }

    fun importLibrary(source: Uri) {
        viewModelScope.launch {
            val success = backupRepository.importLibrary(source)
            _backupMessage.value = if (success) "Library berhasil diimpor" else "Impor gagal"
        }
    }

    fun clearBackupMessage() {
        _backupMessage.value = null
    }
}
