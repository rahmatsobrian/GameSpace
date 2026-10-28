package com.android.axion.gamespace.data

import android.content.Context
import com.android.axion.gamespace.gamebar.HudFields

/** Genre game; menentukan preset otomatis (jaringan, termal, HUD, mode kinerja) selama sesi. */
enum class Genre(val id: Int) {
    GENERAL(0), MOBA(1), SHOOTER(2), BATTLE_ROYALE(3), RACING(4), RPG(5);

    companion object {
        fun fromId(id: Int): Genre = values().firstOrNull { it.id == id } ?: GENERAL
    }
}

/**
 * Preset per genre. refreshMode: 0 tidak diubah, 1 tertinggi, 2 hemat (60 Hz).
 * Semua nilai hanya menjadi default: pilihan eksplisit pengguna (profil game atau pengaturan global) menang.
 */
data class GenrePreset(
    val refreshMode: Int,
    val autoRam: Boolean,
    val dnd: Boolean,
    val turbo: Boolean,
    val autoCoolTemp: Int,
    val lagAlert: Boolean,
    val hudFields: Int,
    val touchBoost: Boolean,
    val breakMinutes: Int,
    val matchCueMinutes: Int,
)

object GenrePresets {
    fun of(g: Genre): GenrePreset = when (g) {
        // Fokus kestabilan jaringan dan frame yang konsisten.
        Genre.MOBA -> GenrePreset(
            refreshMode = 0, autoRam = true, dnd = true, turbo = false, autoCoolTemp = 0,
            lagAlert = true,
            hudFields = HudFields.FPS or HudFields.PING or HudFields.NET or HudFields.MATCH or HudFields.TIMERS,
            touchBoost = true, breakMinutes = 0, matchCueMinutes = 0,
        )
        // Respons secepat mungkin: refresh tertinggi, Turbo, jaringan, dan suhu.
        Genre.SHOOTER -> GenrePreset(
            refreshMode = 1, autoRam = true, dnd = true, turbo = true, autoCoolTemp = 46,
            lagAlert = true,
            hudFields = HudFields.FPS or HudFields.PING or HudFields.NET or HudFields.CPU or HudFields.GPU or HudFields.TEMP,
            touchBoost = true, breakMinutes = 0, matchCueMinutes = 0,
        )
        // Match panjang: sama seperti Shooter ditambah timer match.
        Genre.BATTLE_ROYALE -> GenrePreset(
            refreshMode = 1, autoRam = true, dnd = true, turbo = true, autoCoolTemp = 46,
            lagAlert = true,
            hudFields = HudFields.FPS or HudFields.PING or HudFields.NET or HudFields.TEMP or HudFields.MATCH or HudFields.TIMERS,
            touchBoost = true, breakMinutes = 0, matchCueMinutes = 0,
        )
        Genre.RACING -> GenrePreset(
            refreshMode = 1, autoRam = true, dnd = true, turbo = true, autoCoolTemp = 46,
            lagAlert = false,
            hudFields = HudFields.FPS or HudFields.CPU or HudFields.GPU or HudFields.TEMP,
            touchBoost = false, breakMinutes = 0, matchCueMinutes = 0,
        )
        // Sesi panjang dan berat: hemat daya, jaga suhu, pengingat istirahat.
        Genre.RPG -> GenrePreset(
            refreshMode = 2, autoRam = true, dnd = true, turbo = false, autoCoolTemp = 44,
            lagAlert = false,
            hudFields = HudFields.FPS or HudFields.TEMP or HudFields.POWER or HudFields.RAM,
            touchBoost = false, breakMinutes = 60, matchCueMinutes = 0,
        )
        Genre.GENERAL -> GenrePreset(
            refreshMode = 0, autoRam = false, dnd = false, turbo = false, autoCoolTemp = 0,
            lagAlert = false, hudFields = HudFields.DEFAULT,
            touchBoost = false, breakMinutes = 0, matchCueMinutes = 0,
        )
    }
}

object GenreRegistry {
    /**
     * Daftar bawaan yang terbatas (nama paket game populer). Game lain dianggap Umum;
     * pengguna bisa memilih genre manual per game di layar detail.
     */
    private val known: Map<String, Genre> = mapOf(
        "com.mobile.legends" to Genre.MOBA,
        "com.riotgames.league.wildrift" to Genre.MOBA,
        "com.tencent.tmgp.sgame" to Genre.MOBA,
        "com.levelinfinite.sgameGlobal" to Genre.MOBA,

        "com.activision.callofduty.shooter" to Genre.SHOOTER,
        "com.axlebolt.standoff2" to Genre.SHOOTER,
        "com.criticalforceentertainment.criticalops" to Genre.SHOOTER,

        "com.dts.freefireth" to Genre.BATTLE_ROYALE,
        "com.dts.freefiremax" to Genre.BATTLE_ROYALE,
        "com.tencent.ig" to Genre.BATTLE_ROYALE,
        "com.pubg.imobile" to Genre.BATTLE_ROYALE,
        "com.vng.pubgmobile" to Genre.BATTLE_ROYALE,
        "com.rekoo.pubgm" to Genre.BATTLE_ROYALE,

        "com.gameloft.android.ANMP.GloftA9HM" to Genre.RACING,
        "com.naturalmotion.customstreetracer2" to Genre.RACING,
        "com.ea.games.r3_row" to Genre.RACING,

        "com.miHoYo.GenshinImpact" to Genre.RPG,
        "com.HoYoverse.hkrpgoversea" to Genre.RPG,
        "com.HoYoverse.Nap" to Genre.RPG,
        "com.kurogame.wutheringwaves.global" to Genre.RPG,
        "com.plarium.raidlegends" to Genre.RPG,
    )

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("game_genre", Context.MODE_PRIVATE)

    /** -1 = otomatis. */
    fun override(ctx: Context, pkg: String): Int = sp(ctx).getInt("${pkg}_genre", -1)

    fun setOverride(ctx: Context, pkg: String, id: Int) = sp(ctx).edit().putInt("${pkg}_genre", id).apply()

    fun detect(pkg: String): Genre = known[pkg] ?: Genre.GENERAL

    fun resolve(ctx: Context, pkg: String): Genre {
        val o = override(ctx, pkg)
        return if (o >= 0) Genre.fromId(o) else detect(pkg)
    }
}
