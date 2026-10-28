/*
 * Copyright (C) 2021 Chaldeaprjkt
 * Copyright (C) 2023 risingOS Android Project
 *               2022 crDroid Android Project
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
package com.android.axion.gamespace.data

import android.app.Service
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.view.WindowManager
import androidx.preference.PreferenceManager
import com.android.axion.gamespace.utils.dp
import com.android.axion.gamespace.utils.statusbarHeight
import javax.inject.Inject

class AppSettings @Inject constructor(private val context: Context) {

    private val db by lazy { PreferenceManager.getDefaultSharedPreferences(context) }
    private val wm by lazy { context.getSystemService(Service.WINDOW_SERVICE) as WindowManager }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        db.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        db.unregisterOnSharedPreferenceChangeListener(listener)
    }

    var x
        get() = db.getInt("offset_x", wm.maximumWindowMetrics.bounds.width() / 2)
        set(point) = db.edit().putInt("offset_x", point).apply()

    var y
        get() = db.getInt("offset_y", context.statusbarHeight + 8.dp)
        set(point) = db.edit().putInt("offset_y", point).apply()

    var showFps
        get() = db.getBoolean("show_fps", false)
        set(point) = db.edit().putBoolean("show_fps", point).apply()

    var noAutoBrightness
        get() = db.getBoolean(KEY_AUTO_BRIGHTNESS_DISABLE, true)
        set(it) = db.edit().putBoolean(KEY_AUTO_BRIGHTNESS_DISABLE, it).apply()

    var noThreeScreenshot
        get() = db.getBoolean(KEY_3SCREENSHOT_DISABLE, false)
        set(it) = db.edit().putBoolean(KEY_3SCREENSHOT_DISABLE, it).apply()

    var danmakuNotification
        get() = db.getBoolean(KEY_DANMAKU_NOTIFICATION_MODE, true)
        set(value) = db.edit().putBoolean(KEY_DANMAKU_NOTIFICATION_MODE, value).apply()

    var callsMode: Int
        get() = db.getString(KEY_CALLS_MODE, "0")?.toIntOrNull() ?: 0
        set(value) = db.edit().putString(KEY_CALLS_MODE, value.toString()).apply()

    var ringerMode: Int
        get() = db.getString(KEY_RINGER_MODE, "3")?.toIntOrNull() ?: 3
        set(value) = db.edit().putString(KEY_RINGER_MODE, value.toString()).apply()

    var menuOpacity: Int
        get() = db.getInt(KEY_MENU_OPACITY, 100)
        set(value) = db.edit().putInt(KEY_MENU_OPACITY, value).apply()

    var tileOrder: List<String>
        get() = db.getString(KEY_TILE_ORDER, null)?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        set(value) = db.edit().putString(KEY_TILE_ORDER, value.joinToString(",")).apply()

    var brightnessEnabled: Boolean
        get() = db.getBoolean(KEY_BRIGHTNESS_ENABLED, true)
        set(value) = db.edit().putBoolean(KEY_BRIGHTNESS_ENABLED, value).apply()

    var fpsGraphEnabled: Boolean
        get() = db.getBoolean(KEY_FPS_GRAPH_ENABLED, true)
        set(value) = db.edit().putBoolean(KEY_FPS_GRAPH_ENABLED, value).apply()
        
    var quickStartApps: String
        get() = db.getString(KEY_QUICK_START_APPS, "") ?: ""
        set(value) = db.edit().putString(KEY_QUICK_START_APPS, value).apply()

    var callOverlayEnabled
        get() = db.getBoolean(KEY_CALL_OVERLAY_ENABLED, true)
        set(point) = db.edit().putBoolean(KEY_CALL_OVERLAY_ENABLED, point).apply()

    var iconIdleAlpha: Int
        get() = db.getInt(KEY_ICON_IDLE_ALPHA, 25)
        set(value) = db.edit().putInt(KEY_ICON_IDLE_ALPHA, value).apply()

    var autoDnd: Boolean
        get() = db.getBoolean(KEY_AUTO_DND, false)
        set(value) = db.edit().putBoolean(KEY_AUTO_DND, value).apply()

    var activeGamePackage: String? = null

    private fun getPrefKey(baseKey: String): String {
        val pkg = activeGamePackage
        return if (pkg != null) "${pkg}_${baseKey}" else baseKey
    }

    var crosshairEnabled: Boolean
        get() = db.getBoolean(getPrefKey(KEY_CROSSHAIR_ENABLED), db.getBoolean(KEY_CROSSHAIR_ENABLED, false))
        set(value) = db.edit().putBoolean(getPrefKey(KEY_CROSSHAIR_ENABLED), value).apply()

    var crosshairStyle: Int
        get() = db.getInt(getPrefKey(KEY_CROSSHAIR_STYLE), db.getInt(KEY_CROSSHAIR_STYLE, 1))
        set(value) = db.edit().putInt(getPrefKey(KEY_CROSSHAIR_STYLE), value).apply()

    var crosshairSize: Int
        get() = db.getInt(getPrefKey(KEY_CROSSHAIR_SIZE), db.getInt(KEY_CROSSHAIR_SIZE, 36))
        set(value) = db.edit().putInt(getPrefKey(KEY_CROSSHAIR_SIZE), value).apply()

    var crosshairColor: Int
        get() = db.getInt(getPrefKey(KEY_CROSSHAIR_COLOR), db.getInt(KEY_CROSSHAIR_COLOR, 0xFF00FF00.toInt()))
        set(value) = db.edit().putInt(getPrefKey(KEY_CROSSHAIR_COLOR), value).apply()

    var crosshairOpacity: Float
        get() = db.getFloat(getPrefKey(KEY_CROSSHAIR_OPACITY), db.getFloat(KEY_CROSSHAIR_OPACITY, 1f))
        set(value) = db.edit().putFloat(getPrefKey(KEY_CROSSHAIR_OPACITY), value).apply()

    var crosshairOffsetX: Int
        get() = db.getInt(getPrefKey(KEY_CROSSHAIR_OFFSET_X), db.getInt(KEY_CROSSHAIR_OFFSET_X, 0))
        set(value) = db.edit().putInt(getPrefKey(KEY_CROSSHAIR_OFFSET_X), value).apply()

    var crosshairOffsetY: Int
        get() = db.getInt(getPrefKey(KEY_CROSSHAIR_OFFSET_Y), db.getInt(KEY_CROSSHAIR_OFFSET_Y, 0))
        set(value) = db.edit().putInt(getPrefKey(KEY_CROSSHAIR_OFFSET_Y), value).apply()

    var autoBoostMemory: Boolean
        get() = db.getBoolean(KEY_AUTO_BOOST_MEMORY, false)
        set(value) = db.edit().putBoolean(KEY_AUTO_BOOST_MEMORY, value).apply()

    var refreshRateLock: Int
        get() = db.getInt(KEY_REFRESH_RATE_LOCK, 0)
        set(value) = db.edit().putInt(KEY_REFRESH_RATE_LOCK, value).apply()

    var chargeLimitEnabled: Boolean
        get() = db.getBoolean(KEY_CHARGE_LIMIT_ENABLED, false)
        set(value) = db.edit().putBoolean(KEY_CHARGE_LIMIT_ENABLED, value).apply()

    var chargeLimit: Int
        get() = db.getInt(KEY_CHARGE_LIMIT, 80)
        set(value) = db.edit().putInt(KEY_CHARGE_LIMIT, value).apply()

    var breakReminderMinutes: Int
        get() = db.getInt(KEY_BREAK_REMINDER, 0)
        set(value) = db.edit().putInt(KEY_BREAK_REMINDER, value).apply()

    var hudEnabled: Boolean
        get() = db.getBoolean(KEY_HUD_ENABLED, false)
        set(value) = db.edit().putBoolean(KEY_HUD_ENABLED, value).apply()

    /** 0 kiri-atas, 1 kanan-atas, 2 kiri-bawah, 3 kanan-bawah */
    var hudPosition: Int
        get() = db.getInt(KEY_HUD_POSITION, 0)
        set(value) = db.edit().putInt(KEY_HUD_POSITION, value).apply()

    /** Bit: 1 FPS, 2 ping, 4 CPU, 8 suhu, 16 RAM */
    var hudFields: Int
        get() = db.getInt(KEY_HUD_FIELDS, 11)
        set(value) = db.edit().putInt(KEY_HUD_FIELDS, value).apply()

    var hudOpacity: Int
        get() = db.getInt(KEY_HUD_OPACITY, 80)
        set(value) = db.edit().putInt(KEY_HUD_OPACITY, value).apply()

    var alertTemp: Int
        get() = db.getInt(KEY_ALERT_TEMP, 0)
        set(value) = db.edit().putInt(KEY_ALERT_TEMP, value).apply()

    var alertBattery: Int
        get() = db.getInt(KEY_ALERT_BATTERY, 0)
        set(value) = db.edit().putInt(KEY_ALERT_BATTERY, value).apply()

    var pingTarget: Int
        get() = db.getInt(KEY_PING_TARGET, 0)
        set(value) = db.edit().putInt(KEY_PING_TARGET, value).apply()

    var knownTileIds: Set<String>
        get() = db.getString(KEY_KNOWN_TILES, "")?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        set(value) = db.edit().putString(KEY_KNOWN_TILES, value.joinToString(",")).apply()

    var genrePresetsEnabled: Boolean
        get() = db.getBoolean(KEY_GENRE_PRESETS, true)
        set(value) = db.edit().putBoolean(KEY_GENRE_PRESETS, value).apply()

    var lagAlertEnabled: Boolean
        get() = db.getBoolean(KEY_LAG_ALERT, false)
        set(value) = db.edit().putBoolean(KEY_LAG_ALERT, value).apply()

    var autoCoolTemp: Int
        get() = db.getInt(KEY_AUTO_COOL, 0)
        set(value) = db.edit().putInt(KEY_AUTO_COOL, value).apply()

    var matchCueMinutes: Int
        get() = db.getInt(KEY_MATCH_CUE, 0)
        set(value) = db.edit().putInt(KEY_MATCH_CUE, value).apply()

    /** 0 = isi HUD manual, 1 = ikut genre game. */
    var hudFieldsMode: Int
        get() = db.getInt(KEY_HUD_MODE, 1)
        set(value) = db.edit().putInt(KEY_HUD_MODE, value).apply()

    fun timerSeconds(index: Int): Int = db.getInt("gamespace_timer_$index", intArrayOf(60, 120, 180)[index])

    fun setTimerSeconds(index: Int, seconds: Int) = db.edit().putInt("gamespace_timer_$index", seconds).apply()

    var musicPlayerEnabled: Boolean
        get() = db.getBoolean(KEY_MUSIC_PLAYER_ENABLED, true)
        set(value) = db.edit().putBoolean(KEY_MUSIC_PLAYER_ENABLED, value).apply()

    companion object {
        const val KEY_AUTO_BRIGHTNESS_DISABLE = "gamespace_auto_brightness_disabled"
        const val KEY_3SCREENSHOT_DISABLE = "gamespace_tfgesture_disabled"
        const val KEY_STAY_AWAKE = "gamespace_stay_awake"
        const val KEY_DANMAKU_NOTIFICATION_MODE = "gamespace_danmaku_notification_mode"
        const val KEY_CALLS_MODE = "gamespace_calls_mode"
        const val KEY_RINGER_MODE = "gamespace_ringer_mode"
        const val KEY_LOCK_GESTURE = "gamespace_lock_gesture"
        const val KEY_MENU_OPACITY = "gamespace_menu_opacity"
        const val KEY_TILE_ORDER = "tile_order"
        const val KEY_BRIGHTNESS_ENABLED = "brightness_enabled"
        const val KEY_FPS_GRAPH_ENABLED = "fps_graph_enabled"
        const val KEY_QUICK_START_APPS = "quick_start_apps"
        const val KEY_CALL_OVERLAY_ENABLED = "call_overlay_enabled"
        const val KEY_ICON_IDLE_ALPHA = "gamespace_icon_idle_alpha"
        const val KEY_AUTO_DND = "gamespace_auto_dnd"
        const val KEY_CROSSHAIR_ENABLED = "gamespace_crosshair_enabled"
        const val KEY_CROSSHAIR_STYLE = "gamespace_crosshair_style"
        const val KEY_CROSSHAIR_SIZE = "gamespace_crosshair_size"
        const val KEY_CROSSHAIR_COLOR = "gamespace_crosshair_color"
        const val KEY_CROSSHAIR_OPACITY = "gamespace_crosshair_opacity"
        const val KEY_CROSSHAIR_OFFSET_X = "gamespace_crosshair_offset_x"
        const val KEY_CROSSHAIR_OFFSET_Y = "gamespace_crosshair_offset_y"
        const val KEY_MUSIC_PLAYER_ENABLED = "gamespace_music_player_enabled"
        const val KEY_AUTO_BOOST_MEMORY = "gamespace_auto_boost_memory"
        const val KEY_REFRESH_RATE_LOCK = "gamespace_refresh_rate_lock"
        const val KEY_CHARGE_LIMIT_ENABLED = "gamespace_charge_limit_enabled"
        const val KEY_CHARGE_LIMIT = "gamespace_charge_limit"
        const val KEY_BREAK_REMINDER = "gamespace_break_reminder_minutes"
        const val KEY_HUD_ENABLED = "gamespace_hud_enabled"
        const val KEY_GENRE_PRESETS = "gamespace_genre_presets"
        const val KEY_LAG_ALERT = "gamespace_lag_alert"
        const val KEY_AUTO_COOL = "gamespace_auto_cool_temp"
        const val KEY_MATCH_CUE = "gamespace_match_cue_minutes"
        const val KEY_HUD_MODE = "gamespace_hud_fields_mode"
        const val KEY_HUD_POSITION = "gamespace_hud_position"
        const val KEY_HUD_FIELDS = "gamespace_hud_fields"
        const val KEY_HUD_OPACITY = "gamespace_hud_opacity"
        const val KEY_ALERT_TEMP = "gamespace_alert_temp"
        const val KEY_ALERT_BATTERY = "gamespace_alert_battery"
        const val KEY_PING_TARGET = "gamespace_ping_target"
        const val KEY_KNOWN_TILES = "gamespace_known_tile_ids"
    }
}
