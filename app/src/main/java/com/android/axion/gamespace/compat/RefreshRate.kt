package com.android.axion.gamespace.compat

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.compose.runtime.mutableIntStateOf
import com.android.axion.gamespace.shell.Shell
import kotlin.math.roundToInt

/**
 * Kunci refresh rate lewat setting AOSP `min_refresh_rate` / `peak_refresh_rate` (butuh Shizuku/root).
 * Tidak semua ROM memakai kunci ini; ROM yang punya pengaturan refresh rate sendiri bisa mengabaikannya.
 */
object RefreshRate {
    private const val MIN_KEY = "min_refresh_rate"
    private const val PEAK_KEY = "peak_refresh_rate"

    /** 0 = tidak dikunci. Dibaca oleh tile agar UI ikut berubah. */
    val locked = mutableIntStateOf(0)

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("refresh_rate", Context.MODE_PRIVATE)

    fun sync(ctx: Context) {
        locked.intValue = sp(ctx).getInt("locked", 0)
    }

    fun supported(ctx: Context): List<Int> = try {
        val dm = ctx.getSystemService(DisplayManager::class.java)
        val d = dm.getDisplay(Display.DEFAULT_DISPLAY)
        d?.supportedModes?.map { it.refreshRate.roundToInt() }?.distinct()?.sorted() ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    /** hz > 0: kunci; hz = 0: kembalikan nilai asli. */
    fun apply(ctx: Context, hz: Int) {
        val app = ctx.applicationContext
        locked.intValue = hz
        Shell.background {
            if (!Shell.available()) return@background
            val s = sp(app)
            if (hz > 0) {
                if (!s.contains("orig_min")) {
                    val omin = Shell.exec("settings get system $MIN_KEY").text
                    val opeak = Shell.exec("settings get system $PEAK_KEY").text
                    s.edit().putString("orig_min", omin).putString("orig_peak", opeak).apply()
                }
                Shell.exec("settings put system $MIN_KEY $hz; settings put system $PEAK_KEY $hz")
                s.edit().putInt("locked", hz).apply()
            } else {
                if (s.contains("orig_min")) {
                    fun restore(key: String, orig: String?) {
                        if (orig.isNullOrEmpty() || orig == "null") Shell.exec("settings delete system $key")
                        else Shell.exec("settings put system $key $orig")
                    }
                    restore(MIN_KEY, s.getString("orig_min", null))
                    restore(PEAK_KEY, s.getString("orig_peak", null))
                    s.edit().remove("orig_min").remove("orig_peak").apply()
                }
                s.edit().putInt("locked", 0).apply()
            }
        }
    }
}
