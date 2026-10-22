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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.android.axion.gamespace.compat.HwNodes
import com.android.axion.gamespace.compat.ThreeFingerGesture
import com.android.axion.gamespace.shell.Backend
import com.android.axion.gamespace.shell.Shell
import com.android.axion.gamespace.ui.theme.GameSpaceTheme

/**
 * Layar setup untuk versi universal: Shizuku/root, izin, dan node perangkat keras.
 * Di ROM Axion, semua ini diberikan otomatis karena aplikasi berjalan sebagai aplikasi sistem.
 */
class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Shell.init(this)
        setContent {
            GameSpaceTheme(darkTheme = true) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SetupScreen(onDone = {
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("done", true).apply()
                        finish()
                    })
                }
            }
        }
    }

    companion object {
        private const val PREFS = "setup"

        fun shouldShow(ctx: Context): Boolean {
            val done = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("done", false)
            return !Perms.canOverlay(ctx) || !done && !Shell.available()
        }
    }
}

private class PermRow(val label: String, val ok: Boolean, val action: () -> Unit)

private fun open(ctx: Context, i: Intent) {
    try { ctx.startActivity(i) } catch (_: Exception) {
        try {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
        } catch (_: Exception) {}
    }
}

@Composable
private fun SetupScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val status by Shell.status.collectAsState()
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { Shell.refresh(); tick++ }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }
    val pkg = ctx.packageName
    val pkgUri = Uri.parse("package:$pkg")

    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Setup", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Prioritas utama: Shizuku. Dengan Shizuku (atau root) fitur Game Mode, FPS, bypass charging, " +
                "touch boost, screenshot, dan kontrol memori bisa berjalan.",
            style = MaterialTheme.typography.bodyMedium
        )

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Akses privileged", style = MaterialTheme.typography.titleMedium)
                val line = when {
                    status.backend == Backend.SHIZUKU ->
                        "Shizuku terhubung (UID ${status.uid}: " + (if (status.uid == 0) "root" else "shell") + ")"
                    status.backend == Backend.ROOT -> "Root tersedia (su)"
                    !status.shizukuInstalled -> "Shizuku belum terpasang"
                    !status.shizukuRunning -> "Shizuku terpasang tetapi belum berjalan. Buka aplikasi Shizuku lalu start."
                    !status.shizukuGranted -> "Shizuku berjalan, izin untuk Game Space belum diberikan"
                    else -> "Menghubungkan ke Shizuku…"
                }
                Text(line, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        if (!Shell.openShizukuApp(ctx)) {
                            open(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")))
                        }
                    }) { Text("Buka Shizuku") }
                    Button(
                        onClick = { Shell.requestShizukuPermission() },
                        enabled = status.shizukuRunning && !status.shizukuGranted
                    ) { Text("Beri izin") }
                }
                Button(
                    onClick = {
                        Shell.background {
                            val cmds = listOf(
                                "pm grant $pkg android.permission.WRITE_SECURE_SETTINGS",
                                "pm grant $pkg android.permission.READ_PHONE_STATE",
                                "pm grant $pkg android.permission.ANSWER_PHONE_CALLS",
                                "pm grant $pkg android.permission.READ_CONTACTS",
                                "pm grant $pkg android.permission.POST_NOTIFICATIONS",
                                "appops set $pkg SYSTEM_ALERT_WINDOW allow",
                                "appops set $pkg GET_USAGE_STATS allow",
                                "appops set $pkg WRITE_SETTINGS allow",
                                "appops set $pkg RUN_ANY_IN_BACKGROUND allow",
                                "cmd notification allow_listener ${Perms.listenerComponent(ctx)}",
                                "cmd notification allow_dnd $pkg",
                                "cmd deviceidle whitelist +$pkg",
                            )
                            val failed = cmds.count { !Shell.exec(it).ok }
                            ThreeFingerGesture.refresh(ctx)
                            HwNodes.probe(ctx)
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                Toast.makeText(
                                    ctx,
                                    if (failed == 0) "Semua izin diberikan" else "$failed perintah gagal (cek manual di bawah)",
                                    Toast.LENGTH_LONG
                                ).show()
                                tick++
                            }
                        }
                    },
                    enabled = status.backend != Backend.NONE,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Beri semua izin otomatis") }
            }
        }

        // `tick` memaksa pembacaan ulang status izin setiap kali layar kembali aktif.
        val rows = remember(tick) {
            buildList {
                add(PermRow("Display over other apps (overlay)", Perms.canOverlay(ctx)) {
                    open(ctx, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri))
                })
                add(PermRow("Usage access (deteksi game)", Perms.hasUsageAccess(ctx)) {
                    open(ctx, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                })
                add(PermRow("Modify system settings (brightness)", Perms.canWriteSettings(ctx)) {
                    open(ctx, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkgUri))
                })
                add(PermRow("Do Not Disturb access", Perms.hasDnd(ctx)) {
                    open(ctx, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                })
                add(PermRow("Notification access (danmaku & musik)", Perms.hasListener(ctx)) {
                    open(ctx, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                })
                val runtime = buildList {
                    add(Manifest.permission.READ_PHONE_STATE)
                    add(Manifest.permission.ANSWER_PHONE_CALLS)
                    add(Manifest.permission.READ_CONTACTS)
                    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                }
                add(PermRow("Telepon, kontak, dan notifikasi", runtime.all { Perms.granted(ctx, it) }) {
                    launcher.launch(runtime.toTypedArray())
                })
                add(PermRow("Baterai: tanpa pembatasan", Perms.ignoringBattery(ctx)) {
                    open(ctx, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                })
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Izin", style = MaterialTheme.typography.titleMedium)
                rows.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (r.ok) Text("OK", style = MaterialTheme.typography.bodySmall)
                        else OutlinedButton(onClick = r.action) { Text("Beri") }
                    }
                }
                Text(
                    "Beberapa ROM (HyperOS, ColorOS, Funtouch, XOS, dll.) juga punya pengaturan autostart dan " +
                        "pembatasan baterai sendiri di aplikasi keamanan/baterai bawaan. Aktifkan agar layanan " +
                        "deteksi game tidak dimatikan sistem.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        NodeCard("Bypass charging", HwNodes.Kind.CHARGE, tick)
        NodeCard("Touch boost", HwNodes.Kind.TOUCH, tick)

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Selesai") }
    }
}

@Composable
private fun NodeCard(title: String, kind: HwNodes.Kind, tick: Int) {
    val ctx = LocalContext.current
    val cur = remember(tick) { HwNodes.custom(ctx, kind) }
    var path by remember { mutableStateOf(cur?.path ?: "") }
    var on by remember { mutableStateOf(cur?.on ?: "1") }
    var off by remember { mutableStateOf(cur?.off ?: "0") }
    val resolved = remember(tick) { HwNodes.resolved(ctx, kind) }

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                if (resolved != null) "Node: ${resolved.path} (on=${resolved.on}, off=${resolved.off})"
                else "Node belum ditemukan di perangkat ini. Isi node sysfs/procfs perangkatmu di bawah. " +
                    "Menulisnya butuh root atau Shizuku yang berjalan sebagai root.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = path, onValueChange = { path = it }, singleLine = true,
                label = { Text("Path node") }, modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = on, onValueChange = { on = it }, singleLine = true,
                    label = { Text("Nilai ON") }, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = off, onValueChange = { off = it }, singleLine = true,
                    label = { Text("Nilai OFF") }, modifier = Modifier.weight(1f)
                )
            }
            OutlinedButton(onClick = {
                HwNodes.setCustom(ctx, kind, path, on, off)
                Toast.makeText(ctx, "Tersimpan. Buka ulang aplikasi agar tile/toggle muncul.", Toast.LENGTH_SHORT).show()
            }) { Text("Simpan") }
        }
    }
}
