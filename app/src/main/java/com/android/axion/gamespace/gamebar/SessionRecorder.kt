package com.android.axion.gamespace.gamebar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.android.axion.gamespace.compat.FpsSource
import com.android.axion.gamespace.compat.PingMonitor
import com.android.axion.gamespace.compat.TaskFpsCallback
import com.android.axion.gamespace.data.SessionHistory
import kotlin.math.abs

/** Merekam statistik satu sesi game (durasi, FPS, stabilitas, ping, lonjakan lag, suhu, baterai). */
class SessionRecorder(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var pkg: String? = null
    private var startedAt = 0L
    private var elapsedStart = 0L
    private val samples = ArrayList<Float>()
    private val pings = ArrayList<Int>()
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

    private val pingSampler = object : Runnable {
        override fun run() {
            val p = PingMonitor.ping.value
            if (p >= 0 && pings.size < 3600) pings.add(p)
            main.postDelayed(this, 2000L)
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
        pings.clear()
        maxTemp = 0
        batteryStart = -1
        batteryNow = -1
        ContextCompat.registerReceiver(
            context, batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        main.postDelayed(pingSampler, 2000L)
        if (recordFps) {
            FpsSource.register(fpsCallback, packageName)
            fpsRegistered = true
        }
    }

    /** Harus dipanggil sebelum PingMonitor.stop() agar jumlah lonjakan lag masih terbaca. */
    fun finish() {
        val p = pkg ?: return
        pkg = null
        main.removeCallbacks(pingSampler)
        try { context.unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        if (fpsRegistered) {
            FpsSource.unregister(fpsCallback)
            fpsRegistered = false
        }
        val duration = ((SystemClock.elapsedRealtime() - elapsedStart) / 1000L).toInt()
        if (duration < 30) return // abaikan sesi sangat singkat

        val list = synchronized(samples) { samples.toList() }
        val avg = if (list.isEmpty()) 0 else list.average().toInt()
        var low1 = 0
        var stability = 0
        if (list.isNotEmpty()) {
            val sorted = list.sorted()
            low1 = sorted.take(maxOf(1, (sorted.size * 0.01f).toInt())).average().toInt()
            val median = sorted[sorted.size / 2]
            if (median > 0f) stability = list.count { abs(it - median) <= median * 0.15f } * 100 / list.size
        }
        val drop = if (batteryStart >= 0 && batteryNow >= 0) (batteryStart - batteryNow).coerceAtLeast(0) else 0
        val avgPing = if (pings.isEmpty()) 0 else pings.average().toInt()
        val spikes = PingMonitor.net.value.spikes
        SessionHistory.add(
            context,
            SessionHistory.Record(p, startedAt, duration, avg, low1, maxTemp, drop, avgPing, spikes, stability)
        )
    }
}
