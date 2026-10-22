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
package com.android.axion.gamespace.utils

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.android.axion.gamespace.shell.Shell
import javax.inject.Inject

class ScreenUtils @Inject constructor(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Screenshot lewat KEYCODE_SYSRQ (120), yang dipetakan ROM ke screenshot layar penuh.
     * Hasil disimpan oleh sistem, jadi URI tidak tersedia (onComplete menerima null).
     */
    fun takeScreenshot(onComplete: ((Uri?) -> Unit)? = null) {
        if (!Shell.available()) {
            Toast.makeText(context, "Butuh Shizuku atau root untuk screenshot", Toast.LENGTH_SHORT).show()
            handler.post { onComplete?.invoke(null) }
            return
        }
        Shell.run("input keyevent 120") { handler.post { onComplete?.invoke(null) } }
    }
}
