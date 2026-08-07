package com.siroha.gamespace.data.quicktoggle

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.provider.Settings
import com.siroha.gamespace.core.privilege.PrivilegeRepository
import com.siroha.gamespace.core.privilege.PrivilegedExecResult
import com.siroha.gamespace.core.settings.WriteSettingsPermissionSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickToggleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val privilegeRepository: PrivilegeRepository,
    private val writeSettingsPermissionSource: WriteSettingsPermissionSource
) : QuickToggleRepository {

    override suspend fun toggleWifi(): QuickToggleOutcome {
        if (privilegeRepository.state.value.hasElevatedAccess) {
            // Reading isWifiEnabled needs no special permission — only
            // WifiManager's setter is what Android 10+ blocked.
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val command = if (wifiManager.isWifiEnabled) "svc wifi disable" else "svc wifi enable"
            val result = privilegeRepository.execPrivileged(command)
            if (result is PrivilegedExecResult.Success) return QuickToggleOutcome.Toggled
        }
        openActivity(Settings.Panel.ACTION_WIFI)
        return QuickToggleOutcome.OpenedSettings
    }

    override suspend fun toggleBluetooth(): QuickToggleOutcome {
        if (privilegeRepository.state.value.hasElevatedAccess) {
            // Read via shell too, not BluetoothAdapter — reading adapter
            // state through the Java API needs BLUETOOTH_CONNECT on API
            // 31+, an entire separate runtime-permission flow this app
            // doesn't otherwise need anywhere. `settings get global
            // bluetooth_on` sidesteps that by going through the same
            // shell the privilege layer already uses for everything else.
            val stateResult = privilegeRepository.execPrivileged("settings get global bluetooth_on")
            val isOn = (stateResult as? PrivilegedExecResult.Success)?.output?.firstOrNull()?.trim() == "1"
            val command = if (isOn) "svc bluetooth disable" else "svc bluetooth enable"
            val result = privilegeRepository.execPrivileged(command)
            if (result is PrivilegedExecResult.Success) return QuickToggleOutcome.Toggled
        }
        // No dedicated Settings.Panel constant for Bluetooth the way WiFi
        // has one (not confident one exists) — full settings screen
        // instead of a compact panel is the trade-off for staying inside
        // APIs actually verified this session.
        openActivity(Settings.ACTION_BLUETOOTH_SETTINGS)
        return QuickToggleOutcome.OpenedSettings
    }

    override fun toggleRotationLock(): QuickToggleOutcome {
        if (!writeSettingsPermissionSource.hasPermission()) {
            writeSettingsPermissionSource.openPermissionSettings()
            return QuickToggleOutcome.OpenedSettings
        }
        val newValue = if (isRotationLocked()) 1 else 0 // currently locked (0) -> unlock (1), and vice versa
        Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, newValue)
        return QuickToggleOutcome.Toggled
    }

    override fun isRotationLocked(): Boolean =
        Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 0

    private fun openActivity(action: String) {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
