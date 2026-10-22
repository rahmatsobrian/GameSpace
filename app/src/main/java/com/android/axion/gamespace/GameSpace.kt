/*
 * Copyright (C) 2021 Chaldeaprjkt
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
package com.android.axion.gamespace

import android.app.Application
import android.util.Log
import com.android.axion.gamespace.compat.HwNodes
import com.android.axion.gamespace.compat.ThreeFingerGesture
import com.android.axion.gamespace.shell.Shell
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp(Application::class)
class GameSpace : Hilt_GameSpace() {

    private val TAG = "GameSpace"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created")
        Shell.init(this)
        Shell.background {
            // tunggu binding Shizuku sebentar, lalu cari node perangkat dan kunci gestur 3 jari
            Thread.sleep(1500)
            HwNodes.probe(this)
            ThreeFingerGesture.refresh(this)
        }
    }
}
