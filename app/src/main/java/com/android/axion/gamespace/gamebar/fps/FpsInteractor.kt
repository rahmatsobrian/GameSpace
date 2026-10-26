/*
 * Copyright (C) 2025 AxionOS
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
package com.android.axion.gamespace.gamebar.fps

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.WindowManager
import com.android.axion.gamespace.compat.FpsSource
import com.android.axion.gamespace.compat.TaskFpsCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import kotlin.math.max
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FpsInteractor @Inject constructor(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _fpsHistory = MutableStateFlow<List<Float>>(emptyList())
    val fpsHistory: StateFlow<List<Float>> get() = _fpsHistory

    private val _dynamicMaxFps = MutableStateFlow(60f)
    val dynamicMaxFps: StateFlow<Float> get() = _dynamicMaxFps

    val maxRefreshRate: Float by lazy {
        wm.defaultDisplay?.mode?.refreshRate ?: 60f
    }

    private val _low1Percent = MutableStateFlow(0f)
    val low1Percent: StateFlow<Float> get() = _low1Percent

    private val rawLock = Any()
    private val rawBuffer = ArrayDeque<Float>(600)

    private val historySize = 100
    private val buffer = ArrayDeque<Float>(historySize)

    private val fpsFlow = MutableSharedFlow<Float>(extraBufferCapacity = 64)

    private val fpsCallback = object : TaskFpsCallback() {
        override fun onFpsReported(fps: Float) {
            fpsFlow.tryEmit(fps)
        }
    }

    init {
        coroutineScope.launch {
            fpsFlow
                .sample(1000) 
                .map { fps ->
                    synchronized(rawLock) {
                        if (fps > 0f) {
                            if (rawBuffer.size >= 600) rawBuffer.removeFirst()
                            rawBuffer.addLast(fps)
                        }
                        if (rawBuffer.isNotEmpty()) {
                            val sorted = rawBuffer.sorted()
                            val n = maxOf(1, (sorted.size * 0.01f).toInt())
                            _low1Percent.value = sorted.take(n).average().toFloat()
                        }
                    }
                    val smoothed = (buffer.lastOrNull() ?: fps) * 0.75f + fps * 0.25f
                    if (buffer.size >= historySize) buffer.removeFirst()
                    buffer.addLast(smoothed)
                    val dynamicMax = buffer.maxOrNull()?.coerceAtLeast(15f) ?: 60f
                    smoothed to dynamicMax
                }
                .flowOn(Dispatchers.Default)
                .collect { (smoothed, dynamicMax) ->
                    _fpsHistory.value = buffer.toList()
                    _dynamicMaxFps.value = dynamicMax
                }
        }
    }

    fun start() {
        synchronized(rawLock) { rawBuffer.clear() }
        _low1Percent.value = 0f
        com.android.axion.gamespace.compat.PingMonitor.start()
        FpsSource.register(fpsCallback, null)
    }

    fun dispose() {
        com.android.axion.gamespace.compat.PingMonitor.stop()
        FpsSource.unregister(fpsCallback)
    }
}
