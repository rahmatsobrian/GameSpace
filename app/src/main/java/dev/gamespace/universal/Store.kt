package dev.gamespace.universal

import android.content.Context

/** Penyimpanan pengaturan dan daftar game (SharedPreferences, tanpa API sistem). */
class Store(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("gamespace", Context.MODE_PRIVATE)

    private fun b(k: String, d: Boolean) = sp.getBoolean(k, d)
    private fun putB(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()
    private fun i(k: String, d: Int) = sp.getInt(k, d)
    private fun putI(k: String, v: Int) = sp.edit().putInt(k, v).apply()

    var games: List<String>
        get() = (sp.getString("games", "") ?: "").split(",").filter { it.isNotBlank() }
        set(v) { sp.edit().putString("games", v.joinToString(",")).apply() }

    var serviceEnabled: Boolean get() = b("service", false); set(v) = putB("service", v)
    var noAutoBrightness: Boolean get() = b("no_auto_brightness", true); set(v) = putB("no_auto_brightness", v)
    var stayAwake: Boolean get() = b("stay_awake", true); set(v) = putB("stay_awake", v)
    var autoDnd: Boolean get() = b("auto_dnd", false); set(v) = putB("auto_dnd", v)
    var danmaku: Boolean get() = b("danmaku", false); set(v) = putB("danmaku", v)
    var crosshair: Boolean get() = b("crosshair", false); set(v) = putB("crosshair", v)

    /** 0 = tidak ada aksi, 1 = jawab otomatis, 2 = tolak otomatis */
    var callsMode: Int get() = i("calls", 0); set(v) = putI("calls", v)

    /** 0 = senyap, 1 = getar, 2 = normal, 3 = tidak diubah */
    var ringerMode: Int get() = i("ringer", 3); set(v) = putI("ringer", v)

    /** Opasitas bubble (5..100) */
    var idleAlpha: Int get() = i("alpha", 60); set(v) = putI("alpha", v)
}
