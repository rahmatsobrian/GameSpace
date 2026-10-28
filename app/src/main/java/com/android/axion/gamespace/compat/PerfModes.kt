package com.android.axion.gamespace.compat

import android.app.GameManager
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.android.axion.gamespace.data.UserGame
import com.android.axion.gamespace.shell.Shell

/**
 * Paket profil siap pakai:
 *  - Turbo: game mode Performance + refresh rate tertinggi + bersihkan RAM.
 *  - Cool: game mode Battery + refresh rate 60 Hz untuk menurunkan suhu.
 * Pengaturan sebelumnya dikembalikan saat dimatikan atau sesi berakhir.
 */
object PerfModes {
    val turboActive = mutableStateOf(false)
    val coolActive = mutableStateOf(false)

    /** Paket game yang sedang aktif; diisi SessionService. */
    @Volatile var currentPackage: String? = null

    private var savedLock = 0

    private fun modeName(mode: Int) = when (mode) {
        GameManager.GAME_MODE_PERFORMANCE -> "performance"
        GameManager.GAME_MODE_BATTERY -> "battery"
        else -> "standard"
    }

    private fun userMode(ctx: Context, pkg: String): Int {
        val raw = ctx.getSharedPreferences("system_settings", Context.MODE_PRIVATE).getString("game_list", "").orEmpty()
        return raw.split(";").filter { it.isNotEmpty() }
            .map { UserGame.fromSettings(it) }
            .firstOrNull { it.packageName == pkg }?.mode ?: GameManager.GAME_MODE_STANDARD
    }

    fun setTurbo(ctx: Context, on: Boolean) {
        val pkg = currentPackage
        if (on == turboActive.value) return
        if (on) {
            if (coolActive.value) setCool(ctx, false)
            savedLock = RefreshRate.locked.intValue
            turboActive.value = true
            val max = RefreshRate.supported(ctx).maxOrNull() ?: 0
            if (max >= 60) RefreshRate.apply(ctx, max)
            if (pkg != null) Shell.run("cmd game mode performance $pkg")
            Shell.background { try { MemoryBooster.release(ctx) } catch (_: Exception) {} }
        } else {
            turboActive.value = false
            RefreshRate.apply(ctx, savedLock)
            if (pkg != null) Shell.run("cmd game mode ${modeName(userMode(ctx, pkg))} $pkg")
        }
    }

    fun setCool(ctx: Context, on: Boolean) {
        val pkg = currentPackage
        if (on == coolActive.value) return
        if (on) {
            if (turboActive.value) setTurbo(ctx, false)
            savedLock = RefreshRate.locked.intValue
            coolActive.value = true
            val low = RefreshRate.supported(ctx).filter { it >= 60 }.minOrNull() ?: 0
            if (low >= 60) RefreshRate.apply(ctx, low)
            if (pkg != null) Shell.run("cmd game mode battery $pkg")
        } else {
            coolActive.value = false
            RefreshRate.apply(ctx, savedLock)
            if (pkg != null) Shell.run("cmd game mode ${modeName(userMode(ctx, pkg))} $pkg")
        }
    }

    fun reset(ctx: Context) {
        if (turboActive.value) setTurbo(ctx, false)
        if (coolActive.value) setCool(ctx, false)
    }
}
