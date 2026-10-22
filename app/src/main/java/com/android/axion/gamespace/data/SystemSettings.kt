/*
 * Copyright (C) 2021 Chaldeaprjkt
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
package com.android.axion.gamespace.data

import android.content.Context
import android.provider.Settings
import com.android.axion.gamespace.compat.HwNodes
import com.android.axion.gamespace.compat.StayAwake
import com.android.axion.gamespace.compat.Sys
import com.android.axion.gamespace.compat.ThreeFingerGesture
import com.android.axion.gamespace.utils.GameModeUtils
import javax.inject.Inject

/**
 * Versi universal: pengaturan milik GameSpace disimpan di SharedPreferences (bukan Settings.System
 * ROM Axion), sedangkan pengaturan sistem ditulis langsung atau lewat Shizuku/root.
 */
class SystemSettings @Inject constructor(
    private val context: Context,
    private val gameModeUtils: GameModeUtils
) {
    private val resolver = context.contentResolver
    private val db by lazy { context.getSharedPreferences("system_settings", Context.MODE_PRIVATE) }

    var headsup
        get() = Settings.Global.getInt(
            resolver, Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED, 1) == 1
        set(it) {
            Sys.putGlobal(context, Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED, it.toInt().toString())
        }

    var autoBrightness
        get() = Settings.System.getInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        set(auto) {
            Sys.putSystem(
                context, Settings.System.SCREEN_BRIGHTNESS_MODE,
                (if (auto) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL).toString()
            )
        }

    var bypassChargeEnabled
        get() = HwNodes.isEnabled(context, HwNodes.Kind.CHARGE)
        set(value) {
            HwNodes.set(context, HwNodes.Kind.CHARGE, value)
        }

    var stayAwake
        get() = db.getBoolean("stay_awake", false)
        set(value) {
            db.edit().putBoolean("stay_awake", value).apply()
            StayAwake.enabled = value
            StayAwake.update(context)
        }

    var threeScreenshot
        get() = ThreeFingerGesture.get(context)
        set(value) {
            ThreeFingerGesture.set(context, value)
        }

    var userGames
        get() = (db.getString("game_list", "") ?: "")
            .split(";")
            .filter { it.isNotEmpty() }
            .map { UserGame.fromSettings(it) }
        set(games) {
            db.edit().putString("game_list", games.joinToString(";") { it.toString() }).apply()
            gameModeUtils.setupBatteryMode(games.isNotEmpty())
        }

    private fun Boolean.toInt() = if (this) 1 else 0
}
