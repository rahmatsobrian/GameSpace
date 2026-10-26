package com.android.axion.gamespace.compat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Ping jaringan: waktu handshake TCP ke server publik (perkiraan RTT). Tidak butuh Shizuku/root,
 * hanya izin INTERNET. -1 berarti gagal/tidak ada koneksi.
 */
object PingMonitor {
    private val _ping = MutableStateFlow(-1)
    val ping: StateFlow<Int> get() = _ping

    private val exec = Executors.newSingleThreadScheduledExecutor()
    private var future: ScheduledFuture<*>? = null
    private val targets = listOf("1.1.1.1" to 443, "8.8.8.8" to 53)

    @Synchronized
    fun start() {
        if (future != null) return
        future = exec.scheduleWithFixedDelay({ sample() }, 0, 2, TimeUnit.SECONDS)
    }

    @Synchronized
    fun stop() {
        future?.cancel(false)
        future = null
        _ping.value = -1
    }

    private fun sample() {
        for ((host, port) in targets) {
            try {
                Socket().use { s ->
                    val t0 = System.nanoTime()
                    s.connect(InetSocketAddress(host, port), 1500)
                    _ping.value = ((System.nanoTime() - t0) / 1_000_000L).toInt()
                }
                return
            } catch (_: Exception) {
            }
        }
        _ping.value = -1
    }
}
