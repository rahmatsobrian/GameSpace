/*
 * Copyright (C) Chaldeaprjkt
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
package com.android.axion.gamespace.gamebar.mapper

import android.hardware.input.InputManager
import android.view.KeyEvent

/**
 * Versi universal: InputMonitor butuh izin MONITOR_INPUT (signature). Fitur key-mapper dinonaktifkan
 * di versi asli (BuildFlags.MAPPER_ENABLED = false), jadi monitor ini sengaja no-op.
 */
@Suppress("UNUSED_PARAMETER", "unused")
class KeyInputMonitor(
    private val inputManager: InputManager,
    private val onKeyEvent: (KeyEvent) -> Unit,
) {
    fun start() {}
    fun stop() {}
}
