package com.siroha.gamespace.data.booster

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import com.siroha.gamespace.core.privilege.PrivilegeRepository
import com.siroha.gamespace.core.privilege.PrivilegedExecResult
import com.siroha.gamespace.core.settings.NotificationPolicyPermissionSource
import com.siroha.gamespace.core.settings.WriteSettingsPermissionSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BoosterRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val writeSettingsPermissionSource: WriteSettingsPermissionSource,
    private val notificationPolicyPermissionSource: NotificationPolicyPermissionSource,
    private val privilegeRepository: PrivilegeRepository
) : BoosterRepository {

    private val notificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun hasBrightnessPermission(): Boolean = writeSettingsPermissionSource.hasPermission()
    override fun requestBrightnessPermission() = writeSettingsPermissionSource.openPermissionSettings()

    override fun getBrightness(): Int {
        val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        return ((raw * 100) / 255).coerceIn(0, 100)
    }

    override fun setBrightness(percent: Int) {
        if (!writeSettingsPermissionSource.hasPermission()) return
        val raw = ((percent.coerceIn(0, 100) * 255) / 100)
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, raw)
    }

    override fun hasDndPermission(): Boolean = notificationPolicyPermissionSource.hasPermission()
    override fun requestDndPermission() = notificationPolicyPermissionSource.openPermissionSettings()

    override fun isDndEnabled(): Boolean =
        notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

    override fun setDndEnabled(enabled: Boolean) {
        if (!notificationPolicyPermissionSource.hasPermission()) return
        notificationManager.setInterruptionFilter(
            if (enabled) NotificationManager.INTERRUPTION_FILTER_PRIORITY
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
    }

    override suspend fun setAnimationScale(scale: Float): Boolean {
        val value = scale.coerceIn(0f, 10f)
        val keys = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
        return keys.all { key ->
            privilegeRepository.execPrivileged("settings put global $key $value") is PrivilegedExecResult.Success
        }
    }

    override suspend fun setRefreshRate(hz: Int?): Boolean {
        // `settings delete` is the one subcommand here not double-checked
        // against current docs the way `put`/`get` were — if resetting to
        // auto doesn't work but setting a fixed rate does, that's the
        // first thing to check.
        val commands = if (hz == null) {
            listOf("settings delete system peak_refresh_rate", "settings delete system min_refresh_rate")
        } else {
            listOf("settings put system peak_refresh_rate $hz", "settings put system min_refresh_rate $hz")
        }
        return commands.all { cmd -> privilegeRepository.execPrivileged(cmd) is PrivilegedExecResult.Success }
    }
}
