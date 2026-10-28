package com.android.axion.gamespace.data

import android.content.Context

/**
 * Profil otomatis per-game. Nilai -1 = pakai pengaturan global.
 * refreshRate: -1 global, 0 auto, >0 Hz. autoRam: -1 global, 0 mati, 1 hidup. brightness: -1 tidak diubah, 5..100 persen.
 */
object GameProfiles {
    data class Profile(
        val refreshRate: Int = -1,
        val autoRam: Int = -1,
        val brightness: Int = -1,
        val dnd: Int = -1,
        val turbo: Int = -1,
    )

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("game_profiles", Context.MODE_PRIVATE)

    fun get(ctx: Context, pkg: String): Profile {
        val s = sp(ctx)
        return Profile(
            refreshRate = s.getInt("${pkg}_refresh", -1),
            autoRam = s.getInt("${pkg}_ram", -1),
            brightness = s.getInt("${pkg}_brightness", -1),
            dnd = s.getInt("${pkg}_dnd", -1),
            turbo = s.getInt("${pkg}_turbo", -1),
        )
    }

    fun setRefreshRate(ctx: Context, pkg: String, v: Int) = sp(ctx).edit().putInt("${pkg}_refresh", v).apply()
    fun setAutoRam(ctx: Context, pkg: String, v: Int) = sp(ctx).edit().putInt("${pkg}_ram", v).apply()
    fun setBrightness(ctx: Context, pkg: String, v: Int) = sp(ctx).edit().putInt("${pkg}_brightness", v).apply()
    fun setDnd(ctx: Context, pkg: String, v: Int) = sp(ctx).edit().putInt("${pkg}_dnd", v).apply()
    fun setTurbo(ctx: Context, pkg: String, v: Int) = sp(ctx).edit().putInt("${pkg}_turbo", v).apply()
}
