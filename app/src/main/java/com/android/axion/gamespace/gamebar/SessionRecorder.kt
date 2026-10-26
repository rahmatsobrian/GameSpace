package com.android.axion.gamespace.gamebar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.android.axion.gamespace.compat.FpsSource
import com.android.axion.gamespace.compat.TaskFpsCallback
import com.android.axion.gamespace.data.SessionHistory

/** Merekam statistik satu sesi game (durasi, FPS, suhu, baterai) dan menyimpannya saat sesi berakhir. */
class SessionRecorder(private val context: Context) {
    private var pkg: String? = null
    private var startedAt = 0L
    private var elapsedStart = 0L
    private val samples = ArrayList<Float>()
    private var maxTemp = 0
    private var batteryStart = -1
    private var batteryNow = -1
    private var fpsRegistered = false

    private val fpsCallback = object : TaskFpsCallback() {
        override fun onFpsReported(fps: Float) {
            if (fps <= 0f) return
            synchronized(samples) { if (samples.size < 7200) samples.add(fps) }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level >= 0 && scale > 0) {
                batteryNow = level * 100 / scale
                if (batteryStart < 0) batteryStart = batteryNow
            }
            val temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10
            if (temp > maxTemp) maxTemp = temp
        }
    }

    fun start(packageName: String, recordFps: Boolean) {
        finish()
        pkg = packageName
        startedAt = System.currentTimeMillis()
        elapsedStart = SystemClock.elapsedRealtime()
        synchronized(samples) { samples.clear() }
        maxTemp = 0
        batteryStart = -1
        batteryNow = -1
        ContextCompat.registerReceiver(
            context, batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        if (recordFps) {
            FpsSource.register(fpsCallback, packageName)
            fpsRegistered = true
        }
    }

    fun finish() {
        val p = pkg ?: return
        pkg = null
        try { context.unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        if (fpsRegistered) {
            FpsSource.unregister(fpsCallback)
            fpsRegistered = false
        }
        val duration = ((SystemClock.elapsedRealtime() - elapsedStart) / 1000L).toInt()
        if (duration < 30) return // abaikan sesi sangat singkat
        val list = synchronized(samples) { samples.toList() }
        val avg = if (list.isEmpty()) 0 else list.average().toInt()
        val low1 = if (list.isEmpty()) 0 else {
            val sorted = list.sorted()
            sorted.take(maxOf(1, (sorted.size * 0.01f).toInt())).average().toInt()
        }
        val drop = if (batteryStart >= 0 && batteryNow >= 0) (batteryStart - batteryNow).coerceAtLeast(0) else 0
        SessionHistory.add(context, SessionHistory.Record(p, startedAt, duration, avg, low1, maxTemp, drop))
    }
}
