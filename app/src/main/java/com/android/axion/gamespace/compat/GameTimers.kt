package com.android.axion.gamespace.compat

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.runtime.mutableIntStateOf

/**
 * Alat bantu waktu untuk game kompetitif:
 *  - Timer match (di-reset manual saat match baru dimulai).
 *  - 3 timer hitung mundur yang bisa dikustom (mis. cooldown objektif, respawn, fase zona).
 * Ini murni stopwatch/hitung mundur yang dipicu pengguna; tidak membaca isi game.
 */
object GameTimers {
    /** Naik tiap detik saat ada timer aktif, agar tile ikut diperbarui. */
    val tick = mutableIntStateOf(0)

    private val endAt = LongArray(3)
    @Volatile private var matchStart = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var appContext: Context? = null
    private var ticking = false

    fun beginMatch() { matchStart = SystemClock.elapsedRealtime() }
    fun endSession() {
        matchStart = 0L
        for (i in endAt.indices) endAt[i] = 0L
        handler.removeCallbacks(ticker)
        ticking = false
    }

    private fun fmt(totalSec: Long): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    fun matchText(): String =
        if (matchStart == 0L) "" else fmt((SystemClock.elapsedRealtime() - matchStart) / 1000L)

    fun isActive(i: Int): Boolean = endAt[i] > SystemClock.elapsedRealtime()

    fun remainingSec(i: Int): Int =
        maxOf(0L, (endAt[i] - SystemClock.elapsedRealtime() + 999L) / 1000L).toInt()

    fun label(i: Int, durationSec: Int): String {
        tick.intValue // baca state agar UI ikut berubah
        return when {
            isActive(i) -> "T${i + 1} ${fmt(remainingSec(i).toLong())}"
            durationSec <= 0 -> "T${i + 1} -"
            else -> "T${i + 1} ${fmt(durationSec.toLong())}"
        }
    }

    fun activeText(): String {
        val parts = ArrayList<String>()
        for (i in endAt.indices) if (isActive(i)) parts.add("T${i + 1} ${fmt(remainingSec(i).toLong())}")
        return parts.joinToString("  ")
    }

    fun toggle(ctx: Context, i: Int, durationSec: Int) {
        appContext = ctx.applicationContext
        if (isActive(i)) {
            endAt[i] = 0L
        } else if (durationSec > 0) {
            endAt[i] = SystemClock.elapsedRealtime() + durationSec * 1000L
            if (!ticking) {
                ticking = true
                handler.postDelayed(ticker, 1000L)
            }
        }
        tick.intValue = tick.intValue + 1
    }

    private val ticker = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            var any = false
            for (i in endAt.indices) {
                if (endAt[i] == 0L) continue
                if (now >= endAt[i]) {
                    endAt[i] = 0L
                    finished(i)
                } else {
                    any = true
                }
            }
            tick.intValue = tick.intValue + 1
            if (any) handler.postDelayed(this, 1000L) else ticking = false
        }
    }

    private fun finished(i: Int) {
        val c = appContext ?: return
        try {
            Toast.makeText(c, "Timer ${i + 1} selesai", Toast.LENGTH_SHORT).show()
            c.getSystemService(Vibrator::class.java)
                ?.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {
        }
    }
}
