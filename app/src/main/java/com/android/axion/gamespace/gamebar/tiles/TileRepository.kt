/*
 * Copyright (C) 2025-2026 AxionOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.axion.gamespace.gamebar.tiles

import com.android.axion.gamespace.compat.HwNodes
import com.android.axion.gamespace.compat.MemoryBooster
import com.android.axion.gamespace.compat.GameTimers
import com.android.axion.gamespace.compat.PerfModes
import com.android.axion.gamespace.compat.RefreshRate
import com.android.axion.gamespace.gamebar.DanmakuServiceListener
import com.android.axion.gamespace.gamebar.HudBus
import com.android.axion.gamespace.shell.Shell
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.android.axion.platform.AxFeatureState
import com.android.axion.platform.AxPlatformClient
import com.android.axion.platform.AxPlatformFeature
import com.android.axion.gamespace.R
import com.android.axion.gamespace.data.AppSettings
import com.android.axion.gamespace.data.SystemSettings
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

interface TileAction {
    val id: String
    val label: String
    val icon: Int
    val isEnabled: Boolean
    @Composable fun observeEnabled(): State<Boolean>
    fun toggle()
}

class ToggleableTile(
    override val id: String,
    override var label: String,
    override val icon: Int,
    val state: MutableState<Boolean>,
    private val setter: (Boolean) -> Unit,
) : TileAction {
    override val isEnabled: Boolean get() = state.value
    @Composable override fun observeEnabled(): State<Boolean> = rememberUpdatedState(state.value)
    override fun toggle() {
        state.value = !state.value
        setter(state.value)
    }
}

class FixedActionTile(
    override val id: String,
    override val label: String,
    override val icon: Int,
    private val action: () -> Unit,
) : TileAction {
    override val isEnabled: Boolean = false
    @Composable override fun observeEnabled(): State<Boolean> = rememberUpdatedState(false)
    override fun toggle() = action()
}

class PlatformTile(
    override val id: String,
    override val icon: Int,
    private val platform: AxPlatformClient,
) : TileAction {
    val activeState = mutableStateOf(false)
    val labelState = mutableStateOf("")

    override val label: String get() = labelState.value
    override val isEnabled: Boolean get() = activeState.value
    @Composable override fun observeEnabled(): State<Boolean> = rememberUpdatedState(activeState.value)

    override fun toggle() {
        platform.toggle(id)
    }

    fun updateFromState(state: AxFeatureState) {
        activeState.value = state.isActive
        state.label?.takeIf { it.isNotBlank() }?.let { labelState.value = it }
    }
}

private data class PlatformTileSpec(
    val feature: String,
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
)

@Singleton
class TileRepository @Inject constructor(
    private val context: Context,
    private val appSettings: AppSettings,
    private val systemSettings: SystemSettings,
) {
    private lateinit var platform: AxPlatformClient
    private lateinit var defaultTiles: List<TileAction>

    private val platformTiles = mutableMapOf<String, PlatformTile>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val platformExecutor = Executor { command -> mainHandler.post(command) }
    private var platformRegistered = false

    private val platformCallback =
        AxPlatformClient.StateCallback { key, state ->
            platformTiles[key]?.updateFromState(state)
        }

    private val _tileOrder = mutableStateListOf<String>()

    val allAvailableTiles: List<TileAction>
        get() = defaultTiles

    private val _tiles = mutableStateListOf<TileAction>()
    val tiles: SnapshotStateList<TileAction> get() = _tiles

    val isBrightnessVisible: MutableState<Boolean> = mutableStateOf(appSettings.brightnessEnabled)
    val isFpsGraphVisible: MutableState<Boolean> = mutableStateOf(appSettings.fpsGraphEnabled)
    val isMusicPlayerVisible: MutableState<Boolean> = mutableStateOf(appSettings.musicPlayerEnabled)

    fun init(platform: AxPlatformClient) {
        this.platform = platform
        defaultTiles = buildDefaultTiles()
        registerPlatformCallback()
        refreshPlatformStates()
        _tileOrder.clear()
        _tileOrder.addAll(loadTileOrder())
        _tiles.clear()
        _tiles.addAll(_tileOrder.mapNotNull { id -> defaultTiles.find { it.id == id } })
    }

    fun refreshPlatformStates() {
        if (!::platform.isInitialized) return
        platformTiles.values.forEach { tile ->
            val state = platform.getState(tile.id)
            if (!state.isEmpty) tile.updateFromState(state)
        }
    }

    fun dispose() {
        if (!platformRegistered) return
        platform.unregisterCallback(platformCallback)
        platformRegistered = false
    }

    fun setBrightnessEnabled(enabled: Boolean) {
        isBrightnessVisible.value = enabled
        appSettings.brightnessEnabled = enabled
    }

    fun setFpsGraphEnabled(enabled: Boolean) {
        isFpsGraphVisible.value = enabled
        appSettings.fpsGraphEnabled = enabled
    }

    fun setMusicPlayerEnabled(enabled: Boolean) {
        isMusicPlayerVisible.value = enabled
        appSettings.musicPlayerEnabled = enabled
    }

    fun updateTileSelection(selectedIds: List<String>) {
        _tiles.clear()
        _tiles.addAll(selectedIds.mapNotNull { id -> defaultTiles.find { it.id == id } })
        _tileOrder.clear()
        _tileOrder.addAll(selectedIds)
        saveTileOrder()
    }

    private fun registerPlatformCallback() {
        if (platformRegistered) return
        platform.registerCallback(platformExecutor, platformCallback)
        platformRegistered = true
    }

    private fun saveTileOrder() {
        appSettings.tileOrder = _tileOrder
    }

    private fun loadTileOrder(): List<String> {
        val savedOrder = appSettings.tileOrder
        return if (savedOrder.isNotEmpty()) {
            val kept = savedOrder.filter { id -> defaultTiles.any { it.id == id } }.toMutableList()
            val known = appSettings.knownTileIds
            val fresh = NEW_TILE_IDS.filter { id ->
                id !in known && id !in kept && defaultTiles.any { it.id == id }
            }
            if (fresh.isNotEmpty()) {
                kept.addAll(fresh)
                appSettings.tileOrder = kept
            }
            appSettings.knownTileIds = known + NEW_TILE_IDS
            kept
        } else {
            defaultTiles.map { it.id }
        }
    }

    private fun platformTile(spec: PlatformTileSpec): PlatformTile {
        val tile = PlatformTile(
            id = spec.feature,
            icon = spec.iconRes,
            platform = platform,
        )
        tile.labelState.value = context.getString(spec.labelRes)
        platformTiles[spec.feature] = tile
        return tile
    }

    private fun buildDefaultTiles(): List<TileAction> = buildList {
        platformTiles.clear()
        platformTileSpecs.filter { platform.supports(it.feature) }.mapTo(this) { platformTile(it) }

        add(
            ToggleableTile(
                id = "notification",
                label = context.getString(R.string.tile_danmaku),
                icon = R.drawable.materialsymbols_ic_notifications_rounded_filled,
                state = mutableStateOf(appSettings.danmakuNotification),
                setter = {
                    appSettings.danmakuNotification = it
                    systemSettings.headsup = !it
                },
            )
        )

        add(
            ToggleableTile(
                id = "stay_awake",
                label = context.getString(R.string.tile_stay_awake),
                icon = R.drawable.materialsymbols_ic_bedtime_rounded_filled,
                state = mutableStateOf(systemSettings.stayAwake),
                setter = { systemSettings.stayAwake = it },
            )
        )

        add(
            ToggleableTile(
                id = "crosshair",
                label = context.getString(R.string.tile_crosshair),
                icon = R.drawable.materialsymbols_ic_adjust_rounded_filled,
                state = mutableStateOf(appSettings.crosshairEnabled),
                setter = { appSettings.crosshairEnabled = it },
            )
        )

        add(
            ToggleableTile(
                id = "fps_info",
                label = context.getString(R.string.tile_fps_info),
                icon = R.drawable.materialsymbols_ic_bar_chart_rounded_filled,
                state = mutableStateOf(appSettings.showFps),
                setter = { appSettings.showFps = it },
            )
        )

        add(
            FixedActionTile(
                id = "boost_memory",
                label = context.getString(R.string.tile_boost_memory),
                icon = R.drawable.materialsymbols_ic_speed_rounded_filled,
                action = {
                    Shell.background {
                        val freedMb = try {
                            MemoryBooster.release(context)
                        } catch (_: Exception) {
                            -1L
                        }
                        val msg = if (freedMb > 0) {
                            "${context.getString(R.string.boost_memory)} (${freedMb}MB freed)"
                        } else {
                            context.getString(R.string.boost_memory)
                        }
                        mainHandler.post {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            )
        )

        add(
            FixedActionTile(
                id = "settings",
                label = context.getString(R.string.tile_settings),
                icon = R.drawable.materialsymbols_ic_settings_rounded_filled,
                action = {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
            )
        )

        if (RefreshRate.supported(context).count { it >= 60 } > 1) {
            add(RefreshRateTile(context))
        }

        val audio = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        add(
            SimpleTile(
                id = "mute_media",
                icon = R.drawable.materialsymbols_ic_volume_off_rounded_filled,
                labelProvider = { context.getString(R.string.tile_mute) },
                stateProvider = { audio.isStreamMute(android.media.AudioManager.STREAM_MUSIC) },
                onToggle = {
                    try {
                        audio.adjustStreamVolume(
                            android.media.AudioManager.STREAM_MUSIC,
                            android.media.AudioManager.ADJUST_TOGGLE_MUTE, 0
                        )
                    } catch (_: Exception) {}
                },
            )
        )
        add(
            SimpleTile(
                id = "sound_mode",
                icon = R.drawable.materialsymbols_ic_volume_up_rounded_filled,
                labelProvider = {
                    when (audio.ringerMode) {
                        android.media.AudioManager.RINGER_MODE_VIBRATE -> context.getString(R.string.sound_vibrate)
                        android.media.AudioManager.RINGER_MODE_SILENT -> context.getString(R.string.sound_silent)
                        else -> context.getString(R.string.sound_normal)
                    }
                },
                stateProvider = { audio.ringerMode != android.media.AudioManager.RINGER_MODE_NORMAL },
                onToggle = {
                    try {
                        audio.ringerMode = when (audio.ringerMode) {
                            android.media.AudioManager.RINGER_MODE_NORMAL -> android.media.AudioManager.RINGER_MODE_VIBRATE
                            android.media.AudioManager.RINGER_MODE_VIBRATE -> android.media.AudioManager.RINGER_MODE_SILENT
                            else -> android.media.AudioManager.RINGER_MODE_NORMAL
                        }
                    } catch (_: SecurityException) {}
                },
            )
        )
        add(
            FixedActionTile(
                id = "clear_notifications",
                label = context.getString(R.string.tile_clear_notifications),
                icon = R.drawable.materialsymbols_ic_clear_all_rounded_filled,
                action = {
                    try { DanmakuServiceListener.instance?.cancelAllNotifications() } catch (_: Exception) {}
                },
            )
        )
        add(
            SimpleTile(
                id = "turbo_mode",
                icon = R.drawable.materialsymbols_ic_bolt_rounded_filled,
                labelProvider = { context.getString(R.string.tile_turbo) },
                stateProvider = { PerfModes.turboActive.value },
                onToggle = { PerfModes.setTurbo(context, !PerfModes.turboActive.value) },
            )
        )
        add(
            SimpleTile(
                id = "cool_mode",
                icon = R.drawable.materialsymbols_ic_ac_unit_rounded_filled,
                labelProvider = { context.getString(R.string.tile_cool) },
                stateProvider = { PerfModes.coolActive.value },
                onToggle = { PerfModes.setCool(context, !PerfModes.coolActive.value) },
            )
        )
        add(
            FixedActionTile(
                id = "match_reset",
                label = context.getString(R.string.tile_match_reset),
                icon = R.drawable.materialsymbols_ic_restart_alt_rounded_filled,
                action = { GameTimers.beginMatch() },
            )
        )
        for (index in 0..2) {
            add(
                SimpleTile(
                    id = "timer_${index + 1}",
                    icon = R.drawable.materialsymbols_ic_timer_rounded_filled,
                    labelProvider = { GameTimers.label(index, appSettings.timerSeconds(index)) },
                    stateProvider = { GameTimers.tick.intValue >= 0 && GameTimers.isActive(index) },
                    onToggle = { GameTimers.toggle(context, index, appSettings.timerSeconds(index)) },
                )
            )
        }
        val hudState = mutableStateOf(appSettings.hudEnabled)
        add(
            SimpleTile(
                id = "mini_hud",
                icon = R.drawable.materialsymbols_ic_bar_chart_rounded_filled,
                labelProvider = { context.getString(R.string.tile_mini_hud) },
                stateProvider = { hudState.value },
                onToggle = {
                    val v = !appSettings.hudEnabled
                    appSettings.hudEnabled = v
                    hudState.value = v
                    HudBus.handler?.invoke(v)
                },
            )
        )
        if (HwNodes.isSupported(context, HwNodes.Kind.CHARGE)) {
            val chargeState = mutableStateOf(HwNodes.isEnabled(context, HwNodes.Kind.CHARGE))
            add(
                SimpleTile(
                    id = "pause_charging",
                    icon = R.drawable.materialsymbols_ic_battery_charging_full_rounded_filled,
                    labelProvider = { context.getString(R.string.tile_pause_charging) },
                    stateProvider = { chargeState.value },
                    onToggle = {
                        val v = !chargeState.value
                        chargeState.value = v
                        HwNodes.set(context, HwNodes.Kind.CHARGE, v) { ok ->
                            if (!ok) mainHandler.post { chargeState.value = !v }
                        }
                    },
                )
            )
        }

        if (HwNodes.isSupported(context, HwNodes.Kind.TOUCH)) {
            val touchBoostState = mutableStateOf(
                HwNodes.isEnabled(context, HwNodes.Kind.TOUCH)
            )
            add(
                ToggleableTile(
                    id = "touch_boost",
                    label = context.getString(R.string.tile_touch_boost),
                    icon = R.drawable.materialsymbols_ic_touch_app_rounded_filled,
                    state = touchBoostState,
                    setter = {
                        HwNodes.set(context, HwNodes.Kind.TOUCH, it) { ok ->
                            if (!ok) mainHandler.post { touchBoostState.value = !it }
                        }
                        touchBoostState.value = it
                    },
                )
            )
        }
    }

    companion object {
        /** Tile yang ditambahkan belakangan; otomatis dimunculkan sekali untuk pengguna lama. */
        private val NEW_TILE_IDS = listOf(
            "refresh_rate", "show_touches", "pointer_location", "fast_animations", "haptics",
            "mute_media", "sound_mode", "clear_notifications", "turbo_mode", "cool_mode",
            "mini_hud", "pause_charging", "match_reset", "timer_1", "timer_2", "timer_3",
        )

        private val platformTileSpecs = listOf(
            PlatformTileSpec(
                AxPlatformFeature.WIFI,
                R.drawable.materialsymbols_ic_wifi_rounded_filled,
                R.string.tile_wifi,
            ),
            PlatformTileSpec(
                AxPlatformFeature.BLUETOOTH,
                R.drawable.materialsymbols_ic_bluetooth_rounded_filled,
                R.string.tile_bluetooth,
            ),
            PlatformTileSpec(
                AxPlatformFeature.ZEN,
                R.drawable.materialsymbols_ic_do_not_disturb_on_rounded_filled,
                R.string.tile_dnd,
            ),
            PlatformTileSpec(
                AxPlatformFeature.ROTATION,
                R.drawable.materialsymbols_ic_screen_rotation_up_rounded_filled,
                R.string.tile_auto_rotate,
            ),
            PlatformTileSpec(
                AxPlatformFeature.MOBILE_DATA,
                R.drawable.materialsymbols_ic_android_cell_4_bar_rounded_filled,
                R.string.tile_mobile_data,
            ),
            PlatformTileSpec(
                AxPlatformFeature.AIRPLANE_MODE,
                R.drawable.materialsymbols_ic_flight_rounded_filled,
                R.string.tile_airplane_mode,
            ),
            PlatformTileSpec(
                AxPlatformFeature.FLASHLIGHT,
                R.drawable.materialsymbols_ic_flashlight_on_rounded_filled,
                R.string.tile_flashlight,
            ),
            PlatformTileSpec(
                AxPlatformFeature.DARK_MODE,
                R.drawable.materialsymbols_ic_dark_mode_rounded_filled,
                R.string.tile_dark_mode,
            ),
            PlatformTileSpec(
                AxPlatformFeature.LOCATION,
                R.drawable.materialsymbols_ic_location_on_rounded_filled,
                R.string.tile_location,
            ),
            PlatformTileSpec(
                AxPlatformFeature.BATTERY_SAVER,
                R.drawable.materialsymbols_ic_battery_saver_rounded_filled,
                R.string.tile_battery_saver,
            ),
            PlatformTileSpec(
                AxPlatformFeature.HOTSPOT,
                R.drawable.materialsymbols_ic_wifi_tethering_rounded_filled,
                R.string.tile_hotspot,
            ),
            PlatformTileSpec(
                AxPlatformFeature.NFC,
                R.drawable.materialsymbols_ic_nfc_rounded_filled,
                R.string.tile_nfc,
            ),
            PlatformTileSpec(
                AxPlatformFeature.NIGHT_LIGHT,
                R.drawable.materialsymbols_ic_nights_stay_rounded_filled,
                R.string.tile_night_light,
            ),
            PlatformTileSpec(
                AxPlatformFeature.AOD,
                R.drawable.materialsymbols_ic_aod_rounded_filled,
                R.string.tile_aod,
            ),
            PlatformTileSpec(
                AxPlatformFeature.DATA_SAVER,
                R.drawable.materialsymbols_ic_data_saver_on_rounded_filled,
                R.string.tile_data_saver,
            ),
            PlatformTileSpec(
                AxPlatformFeature.COLOR_INVERSION,
                R.drawable.materialsymbols_ic_invert_colors_rounded_filled,
                R.string.tile_color_inversion,
            ),
            PlatformTileSpec(
                AxPlatformFeature.COLOR_CORRECTION,
                R.drawable.materialsymbols_ic_palette_rounded_filled,
                R.string.tile_color_correction,
            ),
            PlatformTileSpec(
                AxPlatformFeature.REDUCE_BRIGHTNESS,
                R.drawable.materialsymbols_ic_brightness_low_rounded_filled,
                R.string.tile_reduce_brightness,
            ),
            PlatformTileSpec(
                AxPlatformFeature.ONE_HANDED_MODE,
                R.drawable.materialsymbols_ic_phone_android_rounded_filled,
                R.string.tile_one_handed,
            ),
            PlatformTileSpec(
                AxPlatformFeature.HEADS_UP,
                R.drawable.materialsymbols_ic_notifications_active_rounded_filled,
                R.string.tile_heads_up,
            ),
            PlatformTileSpec(
                AxPlatformFeature.AUTO_SYNC,
                R.drawable.materialsymbols_ic_sync_rounded_filled,
                R.string.tile_auto_sync,
            ),
            PlatformTileSpec(
                AxPlatformFeature.CAMERA_PRIVACY,
                R.drawable.materialsymbols_ic_photo_camera_rounded_filled,
                R.string.tile_camera_privacy,
            ),
            PlatformTileSpec(
                AxPlatformFeature.MIC_PRIVACY,
                R.drawable.materialsymbols_ic_mic_rounded_filled,
                R.string.tile_mic_privacy,
            ),
            PlatformTileSpec(
                AxPlatformFeature.WORK_PROFILE,
                R.drawable.materialsymbols_ic_work_rounded_filled,
                R.string.tile_work_profile,
            ),
            PlatformTileSpec(
                AxPlatformFeature.USB_TETHER,
                R.drawable.materialsymbols_ic_usb_rounded_filled,
                R.string.tile_usb_tether,
            ),
            PlatformTileSpec(
                AxPlatformFeature.DREAM,
                R.drawable.materialsymbols_ic_bedtime_rounded_filled,
                R.string.tile_dream,
            ),
            PlatformTileSpec(
                AxPlatformFeature.READING_MODE,
                R.drawable.materialsymbols_ic_menu_book_rounded_filled,
                R.string.tile_reading_mode,
            ),
            PlatformTileSpec(
                AxPlatformFeature.POWER_SHARE,
                R.drawable.materialsymbols_ic_battery_charging_full_rounded_filled,
                R.string.tile_power_share,
            ),
            PlatformTileSpec(
                AxPlatformFeature.CAFFEINE,
                R.drawable.materialsymbols_ic_local_cafe_rounded_filled,
                R.string.tile_caffeine,
            ),
            PlatformTileSpec(
                AxPlatformFeature.VPN,
                R.drawable.materialsymbols_ic_vpn_key_rounded_filled,
                R.string.tile_vpn,
            ),
            PlatformTileSpec(
                AxPlatformFeature.CAST,
                R.drawable.materialsymbols_ic_cast_rounded_filled,
                R.string.tile_cast,
            ),
            PlatformTileSpec(
                AxPlatformFeature.PROFILES,
                R.drawable.materialsymbols_ic_manage_accounts_rounded_filled,
                R.string.tile_profiles,
            ),
            PlatformTileSpec(
                AxPlatformFeature.SMART_PIXELS,
                R.drawable.materialsymbols_ic_grid_on_rounded_filled,
                R.string.tile_smart_pixels,
            ),
            PlatformTileSpec(
                AxPlatformFeature.SCREEN_RECORD,
                R.drawable.materialsymbols_ic_videocam_rounded_filled,
                R.string.tile_screen_record,
            ),
            PlatformTileSpec(
                AxPlatformFeature.SCREENSHOT,
                R.drawable.materialsymbols_ic_screenshot_rounded_filled,
                R.string.tile_screenshot,
            ),
            PlatformTileSpec(
                AxPlatformFeature.SHOW_TOUCHES,
                R.drawable.materialsymbols_ic_touch_app_rounded_filled,
                R.string.tile_show_touches,
            ),
            PlatformTileSpec(
                AxPlatformFeature.POINTER_LOCATION,
                R.drawable.materialsymbols_ic_my_location_rounded_filled,
                R.string.tile_pointer_location,
            ),
            PlatformTileSpec(
                AxPlatformFeature.FAST_ANIMATIONS,
                R.drawable.materialsymbols_ic_animation_rounded_filled,
                R.string.tile_fast_animations,
            ),
            PlatformTileSpec(
                AxPlatformFeature.HAPTICS,
                R.drawable.materialsymbols_ic_vibration_rounded_filled,
                R.string.tile_haptics,
            ),
        )
    }
}
