package com.android.axion.gamespace.compat

import com.android.axion.gamespace.shell.Shell
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * FPS game lewat `dumpsys SurfaceFlinger --latency <layer>` (butuh Shizuku/root).
 * Nilai = jumlah frame yang ditampilkan dalam 1 detik terakhir (perkiraan, bukan counter sistem).
 */
object FpsSource {
    private val listeners = CopyOnWriteArrayList<TaskFpsCallback>()
    private val exec = Executors.newSingleThreadScheduledExecutor()
    private var future: ScheduledFuture<*>? = null
    @Volatile private var pkg: String? = null
    @Volatile private var layer: String? = null

    /** Diisi SessionService saat sesi game dimulai/berakhir. */
    @Volatile var gamePackage: String? = null
        set(value) { if (field != value) layer = null; field = value }

    @Synchronized
    fun register(cb: TaskFpsCallback, gamePackage: String?) {
        if (gamePackage != null && gamePackage != pkg) { pkg = gamePackage; layer = null }
        if (!listeners.contains(cb)) listeners.add(cb)
        if (future == null) {
            future = exec.scheduleWithFixedDelay({ tick() }, 0, 1000, TimeUnit.MILLISECONDS)
        }
    }

    @Synchronized
    fun unregister(cb: TaskFpsCallback) {
        listeners.remove(cb)
        if (listeners.isEmpty()) {
            future?.cancel(false)
            future = null
        }
    }

    private fun tick() {
        try {
            val p = gamePackage ?: pkg ?: return
            if (!Shell.available()) return
            var l = layer ?: findLayer(p)?.also { layer = it } ?: return
            val q = l.replace("'", "'\\''")
            val r = Shell.exec("dumpsys SurfaceFlinger --latency '$q'", 3000)
            val fps = compute(r.out)
            if (fps == null) { layer = null; return }
            listeners.forEach { it.onFpsReported(fps) }
        } catch (_: Exception) {}
    }

    private fun findLayer(p: String): String? {
        val r = Shell.exec("dumpsys SurfaceFlinger --list", 3000)
        val all = r.out.lines().map { it.trim() }.filter { it.contains(p) }
        return all.firstOrNull { it.startsWith("SurfaceView") && it.contains("BLAST") }
            ?: all.firstOrNull { it.startsWith("SurfaceView") }
            ?: all.firstOrNull { it.contains("BLAST") }
            ?: all.firstOrNull()
    }

    private fun compute(out: String): Float? {
        val lines = out.lines()
        if (lines.size < 2) return null
        val ts = lines.drop(1).mapNotNull { l ->
            val parts = l.trim().split(Regex("\\s+"))
            if (parts.size >= 3) parts[1].toLongOrNull() else null
        }.filter { it > 0L && it < Long.MAX_VALUE / 2 }
        if (ts.isEmpty()) return null
        val last = ts.max()
        val now = System.nanoTime()
        if (now - last > 1_500_000_000L) return 0f
        return ts.count { it > last - 1_000_000_000L }.toFloat()
    }
}
