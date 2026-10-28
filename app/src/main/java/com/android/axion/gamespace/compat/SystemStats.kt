package com.android.axion.gamespace.compat

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.android.axion.gamespace.shell.Shell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Monitor sistem: beban CPU, frekuensi CPU, suhu CPU, beban GPU, RAM, dan daya baterai.
 * CPU/GPU/suhu butuh Shizuku/root (/proc dan /sys dibatasi untuk aplikasi biasa); RAM dan daya baterai
 * memakai API publik. Node GPU berbeda tiap chipset (Adreno, Mali, MediaTek) dan dicoba berurutan.
 * Nilai -1 berarti tidak tersedia.
 */
object SystemStats {
    data class Stats(
        val cpuPercent: Int = -1,
        val cpuTempC: Int = -1,
        val ramPercent: Int = -1,
        val ramUsedMb: Int = -1,
        val ramTotalMb: Int = -1,
        val gpuPercent: Int = -1,
        val cpuFreqMhz: Int = -1,
        val powerMw: Int = -1,
    )

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> get() = _stats

    private val exec = Executors.newSingleThreadScheduledExecutor()
    private var future: ScheduledFuture<*>? = null
    private var prevTotal = 0L
    private var prevIdle = 0L
    private var appContext: Context? = null

    private val thermalWords = listOf("cpu", "soc", "tsens", "big", "little", "cluster", "core", "mtk")

    @Synchronized
    fun start(ctx: Context) {
        appContext = ctx.applicationContext
        if (future != null) return
        prevTotal = 0L
        prevIdle = 0L
        future = exec.scheduleWithFixedDelay({ sample() }, 0, 2, TimeUnit.SECONDS)
    }

    @Synchronized
    fun stop() {
        future?.cancel(false)
        future = null
        _stats.value = Stats()
    }

    private fun sample() {
        try {
            var cpu = -1
            var temp = -1
            var gpu = -1
            var freqMhz = -1
            if (Shell.available()) {
                val script = "head -n 1 /proc/stat; " +
                    "for z in /sys/class/thermal/thermal_zone*; do " +
                    "echo \"T:\$(cat \$z/type 2>/dev/null):\$(cat \$z/temp 2>/dev/null)\"; done; " +
                    "for n in /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage /sys/class/misc/mali0/device/utilisation " +
                    "/sys/kernel/ged/hal/gpu_utilization; do " +
                    "if [ -r \$n ]; then echo \"G:\$(head -n 1 \$n 2>/dev/null)\"; break; fi; done; " +
                    "for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq; do echo \"F:\$(cat \$f 2>/dev/null)\"; done"
                val out = Shell.exec(script, 4000).out
                var maxKhz = 0L
                for (line in out.lines()) {
                    when {
                        line.startsWith("cpu ") -> {
                            val v = line.trim().split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
                            if (v.size >= 5) {
                                val idle = v[3] + v[4]
                                val total = v.take(8).sum()
                                if (prevTotal > 0L) {
                                    val dTotal = total - prevTotal
                                    val dIdle = idle - prevIdle
                                    if (dTotal > 0L) cpu = (((dTotal - dIdle) * 100) / dTotal).toInt().coerceIn(0, 100)
                                }
                                prevTotal = total
                                prevIdle = idle
                            }
                        }
                        line.startsWith("T:") -> {
                            val parts = line.split(":")
                            if (parts.size >= 3) {
                                val type = parts[1].lowercase()
                                val raw = parts[2].trim().toLongOrNull()
                                if (raw != null && thermalWords.any { type.contains(it) }) {
                                    val c = when {
                                        raw >= 1000 -> (raw / 1000).toInt()
                                        raw >= 200 -> (raw / 10).toInt()
                                        else -> raw.toInt()
                                    }
                                    if (c in 10..120 && c > temp) temp = c
                                }
                            }
                        }
                        line.startsWith("G:") -> {
                            val m = Regex("\\d+").find(line.substring(2))
                            val g = m?.value?.toIntOrNull()
                            if (g != null && g in 0..100) gpu = g
                        }
                        line.startsWith("F:") -> {
                            val khz = line.substring(2).trim().toLongOrNull()
                            if (khz != null && khz > maxKhz) maxKhz = khz
                        }
                    }
                }
                if (maxKhz > 0L) freqMhz = (maxKhz / 1000).toInt()
            }

            var ramPct = -1
            var used = -1
            var total = -1
            var power = -1
            appContext?.let { c ->
                val am = c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val mi = ActivityManager.MemoryInfo()
                am.getMemoryInfo(mi)
                if (mi.totalMem > 0) {
                    total = (mi.totalMem / (1024 * 1024)).toInt()
                    used = ((mi.totalMem - mi.availMem) / (1024 * 1024)).toInt()
                    ramPct = (((mi.totalMem - mi.availMem) * 100) / mi.totalMem).toInt()
                }
                try {
                    val bm = c.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val raw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    val sticky: Intent? = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val mv = sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
                    if (raw != 0 && raw != Int.MIN_VALUE && mv > 0) {
                        // Sebagian perangkat melaporkan mikroampere, sebagian miliampere.
                        val ma = if (abs(raw) >= 20_000) abs(raw) / 1000 else abs(raw)
                        power = (ma.toLong() * mv / 1000).toInt()
                    }
                } catch (_: Exception) {
                }
            }
            _stats.value = Stats(
                cpuPercent = cpu, cpuTempC = temp, ramPercent = ramPct, ramUsedMb = used, ramTotalMb = total,
                gpuPercent = gpu, cpuFreqMhz = freqMhz, powerMw = power,
            )
        } catch (_: Exception) {
        }
    }
}
