package com.android.axion.gamespace.compat

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Statistik jaringan dari 30 sampel terakhir (sekitar 1 menit). */
data class NetStats(
    val avg: Int = -1,
    val jitter: Int = -1,
    val loss: Int = 0,
    val max: Int = -1,
    /** 0 baik, 1 sedang, 2 buruk */
    val quality: Int = 0,
    val spikes: Int = 0,
)

/**
 * Ping jaringan: waktu handshake TCP ke server publik (perkiraan RTT). Hanya butuh izin INTERNET.
 * Dari rangkaian sampel dihitung jitter, packet loss, penilaian kualitas, dan lonjakan lag.
 */
object PingMonitor {
    private val _ping = MutableStateFlow(-1)
    val ping: StateFlow<Int> get() = _ping

    private val _net = MutableStateFlow(NetStats())
    val net: StateFlow<NetStats> get() = _net

    private val exec = Executors.newSingleThreadScheduledExecutor()
    private var future: ScheduledFuture<*>? = null
    private val targets = listOf("1.1.1.1" to 443, "8.8.8.8" to 53, "9.9.9.9" to 443)

    /** Indeks server utama (0 Cloudflare, 1 Google, 2 Quad9); yang lain jadi cadangan. */
    @Volatile var preferred = 0

    /** Dipanggil (di thread latar) saat terjadi lonjakan ping; maksimal sekali per 10 detik. */
    @Volatile var onLagSpike: ((Int) -> Unit)? = null

    private val window = ArrayDeque<Int>()
    private var spikes = 0
    private var lastSpikeAt = 0L
    private var lastWasSpike = false

    @Synchronized
    fun start() {
        if (future != null) return
        synchronized(window) { window.clear() }
        spikes = 0
        lastWasSpike = false
        _net.value = NetStats()
        future = exec.scheduleWithFixedDelay({ sample() }, 0, 2, TimeUnit.SECONDS)
    }

    @Synchronized
    fun stop() {
        future?.cancel(false)
        future = null
        _ping.value = -1
    }

    private fun sample() {
        val n = targets.size
        val order = targets.indices.map { targets[(preferred.coerceIn(0, n - 1) + it) % n] }
        for ((host, port) in order) {
            try {
                var ms = -1
                Socket().use { s ->
                    val t0 = System.nanoTime()
                    s.connect(InetSocketAddress(host, port), 1500)
                    ms = ((System.nanoTime() - t0) / 1_000_000L).toInt()
                }
                _ping.value = ms
                record(ms)
                return
            } catch (_: Exception) {
            }
        }
        _ping.value = -1
        record(-1)
    }

    private fun record(ms: Int) {
        val snapshot: List<Int> = synchronized(window) {
            window.addLast(ms)
            if (window.size > 30) window.removeFirst()
            window.toList()
        }
        val ok = snapshot.filter { it >= 0 }
        val total = snapshot.size
        val avg = if (ok.isEmpty()) -1 else ok.average().toInt()
        var jitter = -1
        if (ok.size >= 2) {
            var sum = 0
            for (i in 1 until ok.size) sum += abs(ok[i] - ok[i - 1])
            jitter = sum / (ok.size - 1)
        }
        val loss = if (total == 0) 0 else ((total - ok.size) * 100) / total
        val quality = when {
            loss >= 5 || avg >= 150 || jitter >= 40 -> 2
            loss >= 2 || avg >= 80 || jitter >= 20 -> 1
            else -> 0
        }
        val isSpike = ms >= 0 && avg > 0 && ms >= maxOf(120, avg * 3)
        if (isSpike && !lastWasSpike) {
            spikes++
            val now = SystemClock.elapsedRealtime()
            if (now - lastSpikeAt > 10_000L) {
                lastSpikeAt = now
                try { onLagSpike?.invoke(ms) } catch (_: Exception) {}
            }
        }
        lastWasSpike = isSpike
        _net.value = NetStats(avg, jitter, loss, ok.maxOrNull() ?: -1, quality, spikes)
    }
}
