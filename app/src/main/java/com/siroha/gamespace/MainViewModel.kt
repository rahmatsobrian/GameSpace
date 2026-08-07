package com.siroha.gamespace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siroha.gamespace.data.settings.AppSettings
import com.siroha.gamespace.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    /**
     * Null until DataStore's first real read completes — MainActivity
     * uses that null state to hold off rendering the NavHost rather than
     * briefly showing the wrong start destination (onboarding for a
     * returning user) while waiting on the default [AppSettings] value.
     */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
