package com.android.axion.gamespace.compat

import android.content.Context
import com.android.axion.gamespace.shell.Shell

/**
 * Kontrol perangkat keras lewat node sysfs/procfs (bypass charging, touch boost).
 * Nama node berbeda tiap perangkat, jadi: (1) node kustom dari pengguna, atau (2) daftar kandidat
 * umum yang dicek keberadaannya saat probe. Penulisan butuh Shizuku yang berjalan sebagai root,
 * atau root langsung; Shizuku-adb (UID shell) biasanya ditolak SELinux.
 */
object HwNodes {
    enum class Kind(val id: String) { CHARGE("chg"), TOUCH("touch") }

    class Node(val path: String, val on: String, val off: String)

    /** Kandidat umum. Tidak terverifikasi di semua perangkat; hanya dipakai bila node ada. */
    private val candidates = mapOf(
        Kind.CHARGE to listOf(
            Node("/sys/class/power_supply/battery/input_suspend", "1", "0"),
            Node("/sys/class/power_supply/battery/charging_enabled", "0", "1"),
            Node("/sys/class/power_supply/battery/battery_charging_enabled", "0", "1"),
        ),
        Kind.TOUCH to listOf(
            Node("/proc/touchpanel/game_switch_enable", "1", "0"),
        ),
    )

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("hw_nodes", Context.MODE_PRIVATE)

    fun custom(ctx: Context, kind: Kind): Node? {
        val p = sp(ctx).getString("custom_${kind.id}_path", "")?.trim().orEmpty()
        if (p.isEmpty()) return null
        return Node(
            p,
            sp(ctx).getString("custom_${kind.id}_on", "1") ?: "1",
            sp(ctx).getString("custom_${kind.id}_off", "0") ?: "0",
        )
    }

    fun setCustom(ctx: Context, kind: Kind, path: String, on: String, off: String) {
        sp(ctx).edit()
            .putString("custom_${kind.id}_path", path.trim())
            .putString("custom_${kind.id}_on", on.trim())
            .putString("custom_${kind.id}_off", off.trim())
            .remove("resolved_${kind.id}")
            .apply()
        probe(ctx)
    }

    fun resolved(ctx: Context, kind: Kind): Node? {
        custom(ctx, kind)?.let { return it }
        val s = sp(ctx).getString("resolved_${kind.id}", null) ?: return null
        val parts = s.split("|")
        return if (parts.size == 3) Node(parts[0], parts[1], parts[2]) else null
    }

    fun isSupported(ctx: Context, kind: Kind) = resolved(ctx, kind) != null

    /** Jalankan di background: cari node yang ada pada perangkat ini. */
    fun probe(ctx: Context) {
        Shell.background {
            if (!Shell.available()) return@background
            Kind.values().forEach { kind ->
                if (custom(ctx, kind) != null) return@forEach
                val found = candidates[kind].orEmpty().firstOrNull { n ->
                    Shell.exec("test -e ${n.path}").ok
                }
                sp(ctx).edit().apply {
                    if (found != null) putString("resolved_${kind.id}", "${found.path}|${found.on}|${found.off}")
                    else remove("resolved_${kind.id}")
                }.apply()
            }
        }
    }

    fun isEnabled(ctx: Context, kind: Kind): Boolean = sp(ctx).getBoolean("state_${kind.id}", false)

    fun set(ctx: Context, kind: Kind, enable: Boolean, onResult: ((Boolean) -> Unit)? = null) {
        val n = resolved(ctx, kind) ?: run { onResult?.invoke(false); return }
        val v = if (enable) n.on else n.off
        Shell.run("echo $v > ${n.path}") { r ->
            if (r.ok) sp(ctx).edit().putBoolean("state_${kind.id}", enable).apply()
            onResult?.invoke(r.ok)
        }
    }
}
