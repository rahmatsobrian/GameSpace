package com.android.axion.gamespace.compat

import android.content.Context
import com.android.axion.gamespace.shell.Shell

/**
 * Menonaktifkan gestur screenshot 3 jari milik ROM selama game.
 * Kunci pengaturan berbeda antar ROM, jadi kunci dicari saat runtime: hanya kunci yang SUDAH ADA di
 * perangkat dan namanya mengandung pola "three ... finger/gesture ... screenshot" yang disentuh.
 */
object ThreeFingerGesture {
    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("three_finger", Context.MODE_PRIVATE)

    /** Perbarui daftar kunci + status saat ini (background). */
    fun refresh(ctx: Context) {
        Shell.background {
            if (!Shell.available()) return@background
            val script = "for ns in system secure global; do settings list \$ns 2>/dev/null | " +
                "grep -iE 'three.*(finger|gesture|swipe).*(shot|capture)|(shot|capture).*three|(three|3).?finger.*(shot|capture)' | " +
                "sed \"s/^/\$ns:/\"; done"
            val r = Shell.exec(script, 6000)
            val keys = r.out.lines().map { it.trim() }.filter { it.contains(":") && it.contains("=") }
            sp(ctx).edit()
                .putStringSet("keys", keys.toSet())
                .putBoolean("enabled", keys.any { it.substringAfter("=").trim().let { v -> v != "0" && v != "null" } })
                .apply()
        }
    }

    /** 1 jika gestur aktif (menurut cache terakhir), 0 jika nonaktif/tidak ada. */
    fun get(ctx: Context): Int = if (sp(ctx).getBoolean("enabled", false)) 1 else 0

    fun set(ctx: Context, enabled: Int) {
        val keys = sp(ctx).getStringSet("keys", emptySet()).orEmpty()
        if (keys.isEmpty()) return
        val orig = sp(ctx).getStringSet("orig", emptySet()).orEmpty()
        if (enabled == 0) {
            if (orig.isEmpty()) sp(ctx).edit().putStringSet("orig", keys).apply()
            Shell.run(keys.joinToString("; ") { e ->
                val ns = e.substringBefore(":"); val k = e.substringAfter(":").substringBefore("=")
                "settings put $ns $k 0"
            })
            sp(ctx).edit().putBoolean("enabled", false).apply()
        } else {
            val restore = if (orig.isNotEmpty()) orig else keys
            Shell.run(restore.joinToString("; ") { e ->
                val ns = e.substringBefore(":")
                val k = e.substringAfter(":").substringBefore("=")
                val v = e.substringAfter("=").trim().let { if (it == "0" || it == "null") "1" else it }
                "settings put $ns $k $v"
            })
            sp(ctx).edit().putBoolean("enabled", true).remove("orig").apply()
        }
    }
}
