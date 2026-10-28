package com.android.axion.gamespace.data

import android.content.Context
import com.android.axion.gamespace.compat.RefreshRate

/**
 * Nilai efektif sesi. Urutan prioritas: profil per-game (eksplisit) > pengaturan global > preset genre.
 */
data class SessionPolicy(
    val genre: Genre,
    val refreshHz: Int,
    val autoRam: Boolean,
    val dnd: Boolean,
    val turbo: Boolean,
    val autoCoolTemp: Int,
    val lagAlert: Boolean,
    /** null = pakai isi HUD pilihan pengguna. */
    val hudFields: Int?,
    val touchBoost: Boolean,
    val breakMinutes: Int,
    val matchCueMinutes: Int,
) {
    companion object {
        fun resolve(ctx: Context, pkg: String, app: AppSettings): SessionPolicy {
            val genre = GenreRegistry.resolve(ctx, pkg)
            val preset = GenrePresets.of(if (app.genrePresetsEnabled) genre else Genre.GENERAL)
            val profile = GameProfiles.get(ctx, pkg)
            val rates = RefreshRate.supported(ctx).filter { it >= 60 }

            val refresh = when {
                profile.refreshRate >= 0 -> profile.refreshRate
                app.refreshRateLock > 0 -> app.refreshRateLock
                preset.refreshMode == 1 -> rates.maxOrNull() ?: 0
                preset.refreshMode == 2 -> if (60 in rates) 60 else 0
                else -> 0
            }

            return SessionPolicy(
                genre = genre,
                refreshHz = refresh,
                autoRam = if (profile.autoRam >= 0) profile.autoRam == 1 else (app.autoBoostMemory || preset.autoRam),
                dnd = if (profile.dnd >= 0) profile.dnd == 1 else preset.dnd,
                turbo = if (profile.turbo >= 0) profile.turbo == 1 else preset.turbo,
                autoCoolTemp = if (app.autoCoolTemp > 0) app.autoCoolTemp else preset.autoCoolTemp,
                lagAlert = app.lagAlertEnabled || preset.lagAlert,
                hudFields = if (app.hudFieldsMode == 1) preset.hudFields else null,
                touchBoost = preset.touchBoost,
                breakMinutes = if (app.breakReminderMinutes > 0) app.breakReminderMinutes else preset.breakMinutes,
                matchCueMinutes = if (app.matchCueMinutes > 0) app.matchCueMinutes else preset.matchCueMinutes,
            )
        }
    }
}
