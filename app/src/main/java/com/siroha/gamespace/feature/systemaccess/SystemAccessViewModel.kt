package com.siroha.gamespace.feature.systemaccess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siroha.gamespace.core.privilege.PrivilegeRepository
import com.siroha.gamespace.core.privilege.PrivilegeState
import com.siroha.gamespace.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SystemAccessViewModel @Inject constructor(
    private val privilegeRepository: PrivilegeRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val state: StateFlow<PrivilegeState> = privilegeRepository.state

    init {
        // Non-invasive — Shizuku's real state is read directly; root shows
        // UNKNOWN until the user actually taps to request it (see
        // RootPrivilegeSource for why root can't be peeked without prompting).
        privilegeRepository.refresh()
    }

    fun onRecheck() = privilegeRepository.refresh()

    fun onRequestRoot() {
        viewModelScope.launch { privilegeRepository.requestRoot() }
    }

    fun onRequestShizuku() {
        viewModelScope.launch { privilegeRepository.requestShizuku() }
    }

    /** Called once, when the user leaves this screen for the first time
     *  (see onContinue in NavGraph) — this is what lets Home become the
     *  real start destination on the next launch instead of showing this
     *  onboarding gate every time. */
    fun markOnboardingComplete() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(true) }
    }
}
