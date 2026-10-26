package com.android.axion.gamespace.compat

import android.os.SystemClock
import android.util.Log
import com.android.axion.gamespace.shell.Shell
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * FPS game lewat SurfaceFlinger (butuh Shizuku/root).
 *
 * Algoritma diadaptasi dari FPS Meter (https://github.com/rdevzph, MIT License, (c) 2026 Romel Brosas):
 * resolusi layer lewat "Output Layer" lalu `--list`, hitung frame baru dari `--latency`,
 * cross-check counter `frame=` untuk Vulkan, dan self-healing bila layer 0 FPS dua sampel berturut-turut.
 * Semua perintah disaring di sisi shell agar hasil binder tetap kecil.
 */
object FpsSource {
    private const val TAG = "GSFps"
    private const val PENDING = Long.MAX_VALUE

    private val listeners = CopyOnWriteArrayList<TaskFpsCallback>()
    private val exec = Executors.newSingleThreadScheduledExecutor()
    private var future: ScheduledFuture<*>? = null

    @Volatile private var pkg: String? = null

    /** Diisi SessionService saat sesi game dimulai/berakhir. */
    @Volatile var gamePackage: String? = null

    // State berikut hanya disentuh dari thread executor.
    private var currentPkg: String? = null
    private var layer: String? = null
    private var vulkan = false
    private var lastSample = 0L
    private var lastPresent = 0L
    private var lastFrameCount = -1L
    private var zeroCount = 0
    private val badLayers = HashMap<String, Long>() // layer -> waktu kedaluwarsa (ms, elapsedRealtime)

    @Synchronized
    fun register(cb: TaskFpsCallback, gamePackage: String?) {
        if (gamePackage != null) pkg = gamePackage
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
            exec.execute { resetAll() }
        }
    }

    private fun resetLayer() {
        lastPresent = 0L; lastFrameCount = -1L; zeroCount = 0
    }

    private fun resetAll() {
        currentPkg = null; layer = null; lastSample = 0L; resetLayer(); badLayers.clear()
    }

    private fun notifyAll(fps: Float) {
        listeners.forEach { try { it.onFpsReported(fps) } catch (_: Exception) {} }
    }

    private fun tick() {
        try {
            val p = gamePackage ?: pkg
            if (p == null) { Log.d(TAG, "tick: paket game belum diketahui"); return }
            if (!Shell.available()) { Log.w(TAG, "tick: Shizuku/root belum tersedia"); return }

            val now = SystemClock.elapsedRealtime()
            val elapsed = if (lastSample == 0L) 1000L else (now - lastSample).coerceAtLeast(1L)
            lastSample = now

            if (p != currentPkg) {
                currentPkg = p
                vulkan = detectVulkan(p)
                layer = null
                resetLayer()
                Log.d(TAG, "paket game: $p (vulkan=$vulkan)")
            }

            var l = layer
            if (l == null) {
                l = resolveLayer(p)
                layer = l
                resetLayer()
                Log.d(TAG, "layer untuk $p: $l")
            }
            if (l == null) { notifyAll(0f); return }

            val fps = measure(l, elapsed)
            if (fps <= 0) {
                zeroCount++
                if (zeroCount >= 2) {
                    Log.d(TAG, "layer '$l' 0 FPS $zeroCount sampel, dikecualikan 10 dtk lalu cari ulang")
                    badLayers[l] = now + 10_000L
                    layer = null
                    resetLayer()
                }
            } else {
                zeroCount = 0
            }
            notifyAll(fps.toFloat())
        } catch (e: Exception) {
            Log.e(TAG, "tick gagal", e)
        }
    }

    private fun measure(l: String, elapsedMs: Long): Int {
        val q = l.replace("'", "'\\''")
        val out = Shell.exec("dumpsys SurfaceFlinger --latency '$q'", 4000).out
        val lines = out.lines().filter { it.isNotBlank() }
        var fps = -1

        if (lines.size > 1) {
            val ts = mutableListOf<Long>()
            for (i in 1 until lines.size) {
                val parts = lines[i].trim().split(Regex("\\s+"))
                if (parts.size < 3) continue
                val c1 = parts[1].toLongOrNull() ?: 0L
                val c2 = parts[2].toLongOrNull() ?: 0L
                val t = when {
                    c1 > 0 && c1 != PENDING -> c1
                    c2 > 0 && c2 != PENDING -> c2
                    else -> 0L
                }
                if (t > 0) ts.add(t)
            }
            if (ts.isNotEmpty()) {
                val max = ts.max()
                if (lastPresent == 0L || max < lastPresent) {
                    if (ts.size >= 2) {
                        val dur = (max - ts.min()) / 1_000_000_000f
                        if (dur in 0.01f..2.5f) fps = ((ts.size - 1) / dur).roundToInt()
                    }
                    lastPresent = max
                } else {
                    val newFrames = ts.filter { it > lastPresent }
                    fps = ((newFrames.size * 1000f) / elapsedMs).roundToInt()
                    if (newFrames.isNotEmpty()) lastPresent = newFrames.max()
                }
            }
        }

        // Cross-check counter frame (penting untuk Vulkan atau driver yang tidak mengisi --latency penuh)
        if (fps <= 0 || vulkan) {
            val count = frameCount(l)
            if (count != null && count >= 0) {
                if (lastFrameCount >= 0) {
                    val counterFps = (((count - lastFrameCount).coerceAtLeast(0L)) * 1000f / elapsedMs).roundToInt()
                    if (fps <= 0 && counterFps > 0) fps = counterFps
                    else if (vulkan && counterFps > 0 && fps > 0) fps = if (abs(counterFps - fps) <= 3) fps else counterFps
                }
                lastFrameCount = count
            }
        }
        return fps.coerceAtLeast(0)
    }

    private fun frameCount(l: String): Long? {
        val key = l.substringBefore("#").trim().replace("'", "'\\''")
        val out = Shell.exec(
            "dumpsys SurfaceFlinger | grep -F '$key' | grep 'frame=' | grep -v 'Background for' | head -n 3", 4000
        ).out
        return Regex("frame=(\\d+)").find(out)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun detectVulkan(p: String): Boolean {
        val out = Shell.exec("dumpsys gpu | grep -F -A25 'appPackageName = $p'", 4000).out
        return out.contains("createdVulkanSwapchain = 1") || out.contains("createdVulkanDevice = 1")
    }

    private fun renderable(layer: String, p: String): Boolean {
        if (layer.isEmpty() || !layer.contains(p)) return false
        val bad = listOf(
            "ActivityRecord", "Background for", "Bounds for", "Dim layer",
            "SnapshotStartingWindow", "Transition", "leash"
        )
        return bad.none { layer.contains(it) } && !layer.startsWith("Task=")
    }

    private fun clean(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("RequestedLayerState{")) {
            s = s.removePrefix("RequestedLayerState{")
                .substringBefore(" parentId=").substringBefore(" relativeParentId=").substringBefore(" z=")
                .removeSuffix("}")
        } else if (s.startsWith("Layer [")) {
            s = s.substringAfter("] ").trim()
        }
        return s.trim()
    }

    private fun priority(l: String): Int = when {
        l.contains("SurfaceView", true) && l.contains("BLAST", true) -> 0
        l.contains("SurfaceView", true) -> 1
        l.contains("BLAST", true) || l.contains("BBQ", true) -> 2
        !l.contains("PopupWindow") -> 3
        else -> 4
    }

    /** Kandidat layer: "Output Layer" (nama penuh, termasuk tanda kurung di dalamnya) lalu `--list`. */
    private fun candidates(p: String): List<String> {
        val found = LinkedHashSet<String>()
        try {
            val esc = p.replace(".", "\\.")
            val sf = Shell.exec("dumpsys SurfaceFlinger | grep -E 'Output Layer.*$esc' | head -n 30", 5000).out
            // Ambil sampai kurung tutup TERAKHIR agar nama seperti "...(BLAST)#123" tidak terpotong.
            val rx = Regex("Output Layer[^(]*\\((.*)\\)\\s*$")
            sf.lines().forEach { line ->
                rx.find(line.trim())?.groupValues?.get(1)?.trim()?.let { if (renderable(it, p)) found.add(it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "scan Output Layer gagal", e)
        }
        val list = Shell.exec("dumpsys SurfaceFlinger --list | grep -F '$p'", 4000).out
        list.lines().map { clean(it) }.filter { renderable(it, p) }.forEach { found.add(it) }
        // Layer terbaru biasanya paling belakang; urutkan stabil berdasar prioritas.
        return found.toList().reversed().sortedBy { priority(it) }
    }

    /** Layer valid jika `--latency` mengembalikan baris timestamp (nama harus persis). */
    private fun hasLatency(l: String): Boolean {
        val q = l.replace("'", "'\\''")
        val out = Shell.exec("dumpsys SurfaceFlinger --latency '$q'", 4000).out
        val rows = out.lines().filter { it.isNotBlank() }.drop(1)
        return rows.any { row ->
            val parts = row.trim().split(Regex("\\s+"))
            parts.size >= 3 && (parts[1].toLongOrNull() ?: 0L).let { it > 0 && it != PENDING }
        }
    }

    private fun resolveLayer(p: String): String? {
        val now = SystemClock.elapsedRealtime()
        badLayers.entries.removeAll { it.value <= now }
        val cands = candidates(p)
        Log.d(TAG, "kandidat layer ($p): $cands, dikecualikan: ${badLayers.keys}")
        for (c in cands) {
            if (badLayers.containsKey(c)) continue
            if (hasLatency(c)) return c
            Log.d(TAG, "kandidat tanpa data --latency: $c")
        }
        return null
    }
}
