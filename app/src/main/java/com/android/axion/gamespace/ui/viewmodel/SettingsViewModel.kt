package com.android.axion.gamespace.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.android.axion.gamespace.compat.HwNodes
import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.android.axion.gamespace.data.AppSettings
import com.android.axion.gamespace.data.SystemSettings
import com.android.axion.gamespace.data.UserGame
import com.android.axion.gamespace.utils.GameModeUtils
import javax.inject.Inject

data class RegisteredGame(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val mode: Int
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettings: AppSettings,
    private val systemSettings: SystemSettings,
    private val gameModeUtils: GameModeUtils
) : ViewModel() {

    var callOverlayEnabled by mutableStateOf(appSettings.callOverlayEnabled)
        private set

    var danmakuNotification by mutableStateOf(appSettings.danmakuNotification)
        private set

    var callsMode by mutableIntStateOf(appSettings.callsMode)
        private set

    var ringerMode by mutableIntStateOf(appSettings.ringerMode)
        private set

    var noAutoBrightness by mutableStateOf(appSettings.noAutoBrightness)
        private set

    var noThreeScreenshot by mutableStateOf(appSettings.noThreeScreenshot)
        private set

    var stayAwake by mutableStateOf(systemSettings.stayAwake)
        private set

    var menuOpacity by mutableFloatStateOf(appSettings.menuOpacity.toFloat())
        private set

    var registeredGames by mutableStateOf<List<RegisteredGame>>(emptyList())
        private set

    var quickStartApps by mutableStateOf(appSettings.quickStartApps)
        private set

    var bypassChargeEnabled by mutableStateOf(systemSettings.bypassChargeEnabled)
        private set

    var iconIdleAlpha by mutableFloatStateOf(appSettings.iconIdleAlpha.toFloat())
        private set

    var autoDnd by mutableStateOf(appSettings.autoDnd)
        private set

    var musicPlayerEnabled by mutableStateOf(appSettings.musicPlayerEnabled)
        private set

    var crosshairEnabled by mutableStateOf(appSettings.crosshairEnabled)
        private set

    var crosshairStyle by mutableIntStateOf(appSettings.crosshairStyle)
        private set

    var crosshairSize by mutableIntStateOf(appSettings.crosshairSize)
        private set

    var crosshairColor by mutableIntStateOf(appSettings.crosshairColor)
        private set

    var crosshairOpacity by mutableFloatStateOf(appSettings.crosshairOpacity)
        private set

    var crosshairOffsetX by mutableIntStateOf(appSettings.crosshairOffsetX)
        private set

    var crosshairOffsetY by mutableIntStateOf(appSettings.crosshairOffsetY)
        private set

    val isBypassSupported = HwNodes.isSupported(context, HwNodes.Kind.CHARGE)

    init {
        loadRegisteredGames()
    }

    fun updateCrosshairEnabled(enabled: Boolean) {
        crosshairEnabled = enabled
        appSettings.crosshairEnabled = enabled
    }

    fun updateCrosshairStyle(style: Int) {
        crosshairStyle = style
        appSettings.crosshairStyle = style
    }

    fun updateCrosshairSize(size: Int) {
        crosshairSize = size
        appSettings.crosshairSize = size
    }

    fun updateCrosshairColor(color: Int) {
        crosshairColor = color
        appSettings.crosshairColor = color
    }

    fun updateCrosshairOpacity(opacity: Float) {
        crosshairOpacity = opacity
        appSettings.crosshairOpacity = opacity
    }

    fun updateCrosshairOffsets(x: Int, y: Int) {
        crosshairOffsetX = x
        crosshairOffsetY = y
        appSettings.crosshairOffsetX = x
        appSettings.crosshairOffsetY = y
    }

    fun updateCallOverlay(enabled: Boolean) {
        callOverlayEnabled = enabled
        appSettings.callOverlayEnabled = enabled
    }

    fun updateDanmakuNotification(enabled: Boolean) {
        danmakuNotification = enabled
        appSettings.danmakuNotification = enabled
    }

    fun updateCallsMode(mode: Int) {
        callsMode = mode
        appSettings.callsMode = mode
    }

    fun updateRingerMode(mode: Int) {
        ringerMode = mode
        appSettings.ringerMode = mode
    }

    fun updateNoAutoBrightness(disabled: Boolean) {
        noAutoBrightness = disabled
        appSettings.noAutoBrightness = disabled
    }

    fun updateNoThreeScreenshot(disabled: Boolean) {
        noThreeScreenshot = disabled
        appSettings.noThreeScreenshot = disabled
    }

    fun updateStayAwake(enabled: Boolean) {
        stayAwake = enabled
        systemSettings.stayAwake = enabled
    }

    fun updateMenuOpacity(opacity: Float) {
        menuOpacity = opacity
        appSettings.menuOpacity = opacity.toInt()
    }

    fun updateQuickStartApps(apps: String) {
        quickStartApps = apps
        appSettings.quickStartApps = apps
    }

    fun updateBypassChargeEnabled(enabled: Boolean) {
        bypassChargeEnabled = enabled
        systemSettings.bypassChargeEnabled = enabled
    }

    fun updateMusicPlayerEnabled(enabled: Boolean) {
        musicPlayerEnabled = enabled
        appSettings.musicPlayerEnabled = enabled
    }

    fun updateIconIdleAlpha(alpha: Float) {
        iconIdleAlpha = alpha
        appSettings.iconIdleAlpha = alpha.toInt()
    }

    fun updateAutoDnd(enabled: Boolean) {
        autoDnd = enabled
        appSettings.autoDnd = enabled
    }

    fun loadRegisteredGames() {
        val pm = context.packageManager
        val flags = PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())

        registeredGames = systemSettings.userGames
            .mapNotNull { game ->
                try {
                    val appInfo = pm.getApplicationInfo(game.packageName, flags)
                    RegisteredGame(
                        packageName = game.packageName,
                        label = appInfo.loadLabel(pm).toString(),
                        icon = appInfo.loadIcon(pm),
                        mode = game.mode
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
    }

    fun registerGame(packageName: String) {
        val games = systemSettings.userGames.toMutableList()
        if (!games.any { it.packageName == packageName }) {
            games.add(UserGame(packageName))
            systemSettings.userGames = games
        }
        loadRegisteredGames()
    }

    fun unregisterGame(packageName: String) {
        gameModeUtils.setAngleDriverChoice(packageName, GameModeUtils.DRIVER_CHOICE_DEFAULT)
        val games = systemSettings.userGames.toMutableList()
        games.removeIf { it.packageName == packageName }
        systemSettings.userGames = games
        loadRegisteredGames()
    }

    fun updateGameMode(packageName: String, mode: Int) {
        gameModeUtils.setGameModeFor(packageName, systemSettings, mode)
        loadRegisteredGames()
    }

    val gameModeOptions = listOf(1 to "Standard", 2 to "Performance", 3 to "Battery")

    val angleFeatureAvailable: Boolean
        get() = gameModeUtils.findAnglePackage()?.isEnabled == true && gameModeUtils.isVulkanSupported()

    fun getAngleDriverChoice(packageName: String): String = gameModeUtils.getAngleDriverChoice(packageName)

    fun updateAngleDriverChoice(packageName: String, choice: String) {
        gameModeUtils.setAngleDriverChoice(packageName, choice)
    }

    val angleDriverOptions = listOf(
        GameModeUtils.DRIVER_CHOICE_DEFAULT to "Default",
        GameModeUtils.DRIVER_CHOICE_ANGLE to "ANGLE",
        GameModeUtils.DRIVER_CHOICE_NATIVE to "Native"
    )

    // ---- Fitur tambahan: performa, power, pengingat istirahat ----
    var autoBoostMemory by mutableStateOf(appSettings.autoBoostMemory)
        private set

    fun updateAutoBoostMemory(enabled: Boolean) {
        autoBoostMemory = enabled
        appSettings.autoBoostMemory = enabled
    }

    val supportedRefreshRates: List<Int> by lazy {
        com.android.axion.gamespace.compat.RefreshRate.supported(context).filter { it >= 60 }
    }

    var refreshRateLock by mutableIntStateOf(appSettings.refreshRateLock)
        private set

    fun updateRefreshRateLock(hz: Int) {
        refreshRateLock = hz
        appSettings.refreshRateLock = hz
    }

    var breakReminderMinutes by mutableIntStateOf(appSettings.breakReminderMinutes)
        private set

    fun updateBreakReminder(minutes: Int) {
        breakReminderMinutes = minutes
        appSettings.breakReminderMinutes = minutes
    }

    var chargeLimitEnabled by mutableStateOf(appSettings.chargeLimitEnabled)
        private set

    fun updateChargeLimitEnabled(enabled: Boolean) {
        chargeLimitEnabled = enabled
        appSettings.chargeLimitEnabled = enabled
    }

    var chargeLimit by mutableFloatStateOf(appSettings.chargeLimit.toFloat())
        private set

    fun updateChargeLimit(percent: Float) {
        chargeLimit = percent
        appSettings.chargeLimit = percent.toInt()
    }

    // ---- Monitoring, HUD, peringatan ----
    var pingTarget by mutableIntStateOf(appSettings.pingTarget)
        private set

    fun updatePingTarget(index: Int) {
        pingTarget = index
        appSettings.pingTarget = index
        com.android.axion.gamespace.compat.PingMonitor.preferred = index
    }

    var hudEnabled by mutableStateOf(appSettings.hudEnabled)
        private set

    var hudPosition by mutableIntStateOf(appSettings.hudPosition)
        private set

    var hudFields by mutableIntStateOf(appSettings.hudFields)
        private set

    var hudOpacity by mutableFloatStateOf(appSettings.hudOpacity.toFloat())
        private set

    private fun refreshHud() {
        val h = com.android.axion.gamespace.gamebar.HudBus.handler ?: return
        if (hudEnabled) {
            h(false)
            h(true)
        }
    }

    fun updateHudEnabled(enabled: Boolean) {
        hudEnabled = enabled
        appSettings.hudEnabled = enabled
        com.android.axion.gamespace.gamebar.HudBus.handler?.invoke(enabled)
    }

    fun updateHudPosition(position: Int) {
        hudPosition = position
        appSettings.hudPosition = position
        refreshHud()
    }

    fun updateHudField(bit: Int, on: Boolean) {
        hudFields = if (on) hudFields or bit else hudFields and bit.inv()
        appSettings.hudFields = hudFields
        refreshHud()
    }

    fun updateHudOpacity(percent: Float) {
        hudOpacity = percent
        appSettings.hudOpacity = percent.toInt()
    }

    var alertTemp by mutableIntStateOf(appSettings.alertTemp)
        private set

    fun updateAlertTemp(celsius: Int) {
        alertTemp = celsius
        appSettings.alertTemp = celsius
    }

    var alertBattery by mutableIntStateOf(appSettings.alertBattery)
        private set

    fun updateAlertBattery(percent: Int) {
        alertBattery = percent
        appSettings.alertBattery = percent
    }

    // ---- Dukungan game: preset genre, lag, Auto Cool, timer ----
    var genrePresetsEnabled by mutableStateOf(appSettings.genrePresetsEnabled)
        private set

    fun updateGenrePresets(enabled: Boolean) {
        genrePresetsEnabled = enabled
        appSettings.genrePresetsEnabled = enabled
    }

    var lagAlertEnabled by mutableStateOf(appSettings.lagAlertEnabled)
        private set

    fun updateLagAlert(enabled: Boolean) {
        lagAlertEnabled = enabled
        appSettings.lagAlertEnabled = enabled
    }

    var autoCoolTemp by mutableIntStateOf(appSettings.autoCoolTemp)
        private set

    fun updateAutoCool(celsius: Int) {
        autoCoolTemp = celsius
        appSettings.autoCoolTemp = celsius
    }

    var matchCueMinutes by mutableIntStateOf(appSettings.matchCueMinutes)
        private set

    fun updateMatchCue(minutes: Int) {
        matchCueMinutes = minutes
        appSettings.matchCueMinutes = minutes
    }

    var hudFieldsMode by mutableIntStateOf(appSettings.hudFieldsMode)
        private set

    fun updateHudFieldsMode(mode: Int) {
        hudFieldsMode = mode
        appSettings.hudFieldsMode = mode
    }

    val timers = androidx.compose.runtime.mutableStateListOf(
        appSettings.timerSeconds(0), appSettings.timerSeconds(1), appSettings.timerSeconds(2)
    )

    fun updateTimer(index: Int, seconds: Int) {
        timers[index] = seconds
        appSettings.setTimerSeconds(index, seconds)
    }
}
