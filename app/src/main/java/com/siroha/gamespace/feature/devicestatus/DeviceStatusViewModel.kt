package com.siroha.gamespace.feature.devicestatus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siroha.gamespace.core.overlay.OverlayPermissionSource
import com.siroha.gamespace.core.privilege.PrivilegeRepository
import com.siroha.gamespace.core.privilege.PrivilegeTier
import com.siroha.gamespace.data.monitoring.DeviceSnapshot
import com.siroha.gamespace.data.monitoring.MonitoringRepository
import com.siroha.gamespace.feature.overlay.OverlayService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DeviceStatusViewModel @Inject constructor(
    monitoringRepository: MonitoringRepository,
    privilegeRepository: PrivilegeRepository,
    private val overlayPermissionSource: OverlayPermissionSource,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val snapshot: StateFlow<DeviceSnapshot?> = monitoringRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeTier: StateFlow<PrivilegeTier> = privilegeRepository.state
        .map { it.activeTier }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrivilegeTier.PUBLIC_ONLY)

    private val _hasOverlayPermission = MutableStateFlow(overlayPermissionSource.hasPermission())
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _overlayActive = MutableStateFlow(false)
    val overlayActive: StateFlow<Boolean> = _overlayActive.asStateFlow()

    /** AppOps-style grants don't push a change notification — same reason
     *  HomeViewModel re-checks usage access on resume. */
    fun refreshOverlayPermissionStatus() {
        _hasOverlayPermission.value = overlayPermissionSource.hasPermission()
    }

    fun requestOverlayPermission() = overlayPermissionSource.openPermissionSettings()

    fun toggleOverlay() {
        if (_overlayActive.value) {
            OverlayService.stop(context)
            _overlayActive.value = false
            return
        }
        if (!overlayPermissionSource.hasPermission()) {
            overlayPermissionSource.openPermissionSettings()
            return
        }
        OverlayService.start(context)
        _overlayActive.value = true
    }
}
