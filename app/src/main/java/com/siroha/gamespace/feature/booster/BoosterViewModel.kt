package com.siroha.gamespace.feature.booster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siroha.gamespace.core.privilege.PrivilegeRepository
import com.siroha.gamespace.data.booster.BoosterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BoosterUiState(
    val brightnessPercent: Int = 50,
    val hasBrightnessPermission: Boolean = false,
    val dndEnabled: Boolean = false,
    val hasDndPermission: Boolean = false,
    val animationsReduced: Boolean = false,
    val selectedRefreshRate: Int? = null,
    val hasElevatedAccess: Boolean = false,
    val isApplying: Boolean = false
)

@HiltViewModel
class BoosterViewModel @Inject constructor(
    private val boosterRepository: BoosterRepository,
    privilegeRepository: PrivilegeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BoosterUiState(
            brightnessPercent = boosterRepository.getBrightness(),
            dndEnabled = boosterRepository.isDndEnabled()
        )
    )
    val uiState: StateFlow<BoosterUiState> = _uiState.asStateFlow()

    init {
        refreshPermissions()
        viewModelScope.launch {
            privilegeRepository.state.collect { state ->
                _uiState.update { it.copy(hasElevatedAccess = state.hasElevatedAccess) }
            }
        }
    }

    fun refreshPermissions() {
        _uiState.update {
            it.copy(
                hasBrightnessPermission = boosterRepository.hasBrightnessPermission(),
                hasDndPermission = boosterRepository.hasDndPermission()
            )
        }
    }

    fun onBrightnessChange(percent: Int) {
        if (!_uiState.value.hasBrightnessPermission) {
            boosterRepository.requestBrightnessPermission()
            return
        }
        boosterRepository.setBrightness(percent)
        _uiState.update { it.copy(brightnessPercent = percent) }
    }

    fun requestBrightnessPermission() = boosterRepository.requestBrightnessPermission()

    fun onDndToggle(enabled: Boolean) {
        if (!_uiState.value.hasDndPermission) {
            boosterRepository.requestDndPermission()
            return
        }
        boosterRepository.setDndEnabled(enabled)
        _uiState.update { it.copy(dndEnabled = enabled) }
    }

    fun onAnimationsToggle(reduced: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true) }
            val success = boosterRepository.setAnimationScale(if (reduced) 0.5f else 1f)
            _uiState.update {
                it.copy(isApplying = false, animationsReduced = if (success) reduced else it.animationsReduced)
            }
        }
    }

    fun onRefreshRateSelect(hz: Int?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true) }
            val success = boosterRepository.setRefreshRate(hz)
            _uiState.update {
                it.copy(isApplying = false, selectedRefreshRate = if (success) hz else it.selectedRefreshRate)
            }
        }
    }
}
