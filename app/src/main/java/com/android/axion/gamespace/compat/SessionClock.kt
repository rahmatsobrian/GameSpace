package com.android.axion.gamespace.compat

import android.os.SystemClock

/** Stopwatch sesi game, dibaca oleh header panel. */
object SessionClock {
    @Volatile private var startedAt = 0L

    fun begin() { startedAt = SystemClock.elapsedRealtime() }
    fun end() { startedAt = 0L }

    fun minutes(): Int =
        if (startedAt == 0L) 0 else ((SystemClock.elapsedRealtime() - startedAt) / 60_000L).toInt()

    /** "mm:ss" atau "h:mm:ss"; kosong jika sesi belum mulai. */
    fun text(): String {
        if (startedAt == 0L) return ""
        val s = (SystemClock.elapsedRealtime() - startedAt) / 1000L
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }
}
