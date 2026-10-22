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
package com.android.axion.gamespace.setup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.android.axion.gamespace.R
import com.android.axion.gamespace.compat.HwNodes
import com.android.axion.gamespace.compat.ThreeFingerGesture
import com.android.axion.gamespace.data.AppSettings
import com.android.axion.gamespace.gamebar.SessionService
import com.android.axion.gamespace.settings.SettingsActivity
import com.android.axion.gamespace.shell.Shell
import com.android.axion.gamespace.utils.GameModeUtils

/**
 * Pengganti patch SystemUI pada ROM Axion yang memulai SessionService saat game dibuka.
 * Mendeteksi aplikasi di layar (UsageStats; fallback dumpsys lewat Shizuku/root) dan memulai/menghentikan
 * SessionService untuk game yang terdaftar.
 */
class GameWatcherService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var current: String? = null
    private var active: String? = null
    private var lastQuery = 0L
    private var polling = false

    private val tick = object : Runnable {
        override fun run() {
            poll()
            handler.postDelayed(this, 1500L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Shell.init(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH, getString(R.string.settings_title), NotificationManager.IMPORTANCE_MIN)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, SettingsActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, CH)
            .setContentTitle(getString(R.string.settings_title))
            .setSmallIcon(R.drawable.ic_play)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
        HwNodes.probe(this)
        ThreeFingerGesture.refresh(this)
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    private fun userGames(): Set<String> =
        getSharedPreferences("system_settings", Context.MODE_PRIVATE)
            .getString("game_list", "").orEmpty()
            .split(";").filter { it.isNotEmpty() }
            .map { it.substringBefore("=") }.toSet()

    private fun poll() {
        val pkg = foregroundPackage()
        val games = userGames()
        val target = pkg?.takeIf { it in games }
        if (target != active) {
            if (active != null) {
                stopService(Intent(this, SessionService::class.java))
                active = null
            }
            if (target != null) {
                active = target
                try {
                    startService(
                        Intent(this, SessionService::class.java)
                            .setAction(SessionService.ACTION_START)
                            .putExtra(SessionService.EXTRA_PACKAGE_NAME, target)
                    )
                } catch (_: Exception) {
                    active = null
                }
            }
        }
    }

    /** Paket di layar: UsageStats bila izin ada, jika tidak dumpsys lewat Shizuku/root. */
    private fun foregroundPackage(): String? {
        if (Perms.hasUsageAccess(this)) {
            val usm = getSystemService(UsageStatsManager::class.java)
            val now = System.currentTimeMillis()
            val from = if (lastQuery == 0L) now - 15_000L else lastQuery - 500L
            val events = usm.queryEvents(from, now)
            val e = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                when (e.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> current = e.packageName
                    UsageEvents.Event.ACTIVITY_PAUSED -> if (current == e.packageName) current = null
                }
            }
            lastQuery = now
            return current
        }
        if (Shell.available() && !polling) {
            polling = true
            Shell.background {
                val r = Shell.exec(
                    "dumpsys activity activities | grep -m1 -E 'topResumedActivity|ResumedActivity'", 3000
                )
                val m = Regex("""([A-Za-z0-9_.]+)/[A-Za-z0-9_.$]+""").find(r.out)
                current = m?.groupValues?.get(1)
                polling = false
            }
        }
        return current
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        if (active != null) stopService(Intent(this, SessionService::class.java))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CH = "gamespace_watcher"

        fun start(ctx: Context) {
            try {
                ContextCompat.startForegroundService(ctx, Intent(ctx, GameWatcherService::class.java))
            } catch (_: Exception) {}
        }
    }
}
