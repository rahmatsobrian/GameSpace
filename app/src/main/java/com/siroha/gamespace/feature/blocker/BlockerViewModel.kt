package com.siroha.gamespace.feature.blocker

import androidx.lifecycle.ViewModel
import com.siroha.gamespace.core.blocker.BlockerSettingsStore
import com.siroha.gamespace.core.blocker.CallScreeningRoleSource
import com.siroha.gamespace.core.blocker.NotificationAccessPermissionSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class BlockerUiState(
    val hasNotificationAccess: Boolean = false,
    val notificationBlockingActive: Boolean = false,
    val callScreeningAvailable: Boolean = true,
    val hasCallScreeningRole: Boolean = false,
    val callBlockingActive: Boolean = false
)

@HiltViewModel
class BlockerViewModel @Inject constructor(
    private val blockerSettingsStore: BlockerSettingsStore,
    private val notificationAccessPermissionSource: NotificationAccessPermissionSource,
    private val callScreeningRoleSource: CallScreeningRoleSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BlockerUiState(
            notificationBlockingActive = blockerSettingsStore.isNotificationBlockingActive,
            callBlockingActive = blockerSettingsStore.isCallBlockingActive,
            callScreeningAvailable = callScreeningRoleSource.isAvailable()
        )
    )
    val uiState: StateFlow<BlockerUiState> = _uiState.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        _uiState.update {
            it.copy(
                hasNotificationAccess = notificationAccessPermissionSource.hasPermission(),
                hasCallScreeningRole = callScreeningRoleSource.hasRole()
            )
        }
    }

    fun requestNotificationAccess() = notificationAccessPermissionSource.openPermissionSettings()

    fun onNotificationBlockingToggle(enabled: Boolean) {
        if (!_uiState.value.hasNotificationAccess) {
            requestNotificationAccess()
            return
        }
        blockerSettingsStore.isNotificationBlockingActive = enabled
        _uiState.update { it.copy(notificationBlockingActive = enabled) }
    }

    fun requestCallScreeningRole() = callScreeningRoleSource.requestRole()

    fun onCallBlockingToggle(enabled: Boolean) {
        if (!_uiState.value.hasCallScreeningRole) {
            requestCallScreeningRole()
            return
        }
        blockerSettingsStore.isCallBlockingActive = enabled
        _uiState.update { it.copy(callBlockingActive = enabled) }
    }
}
