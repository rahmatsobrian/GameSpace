package com.android.axion.gamespace.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Riwayat sesi game (maks 300 terakhir), disimpan sebagai JSON di SharedPreferences. */
object SessionHistory {
    data class Record(
        val pkg: String,
        val startedAt: Long,
        val durationSec: Int,
        val avgFps: Int,
        val low1: Int,
        val maxTemp: Int,
        val batteryDrop: Int,
        val avgPing: Int = 0,
        val spikes: Int = 0,
        /** Persen sampel FPS yang berada dalam +-15% dari median (stabilitas frame). */
        val stability: Int = 0,
    )

    data class Summary(
        val sessions: Int = 0,
        val totalSec: Long = 0L,
        val avgFps: Int = 0,
        val low1: Int = 0,
        val maxTemp: Int = 0,
        val avgPing: Int = 0,
        val spikes: Int = 0,
        val stability: Int = 0,
        val last: Record? = null,
    )

    private const val KEY = "records"
    private const val MAX = 300

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("session_history", Context.MODE_PRIVATE)

    @Synchronized
    fun add(ctx: Context, r: Record) {
        val arr = try { JSONArray(sp(ctx).getString(KEY, "[]")) } catch (_: Exception) { JSONArray() }
        arr.put(
            JSONObject()
                .put("p", r.pkg).put("s", r.startedAt).put("d", r.durationSec)
                .put("a", r.avgFps).put("l", r.low1).put("t", r.maxTemp).put("b", r.batteryDrop)
                .put("ap", r.avgPing).put("sp", r.spikes).put("st", r.stability)
        )
        val out = JSONArray()
        for (i in maxOf(0, arr.length() - MAX) until arr.length()) out.put(arr.get(i))
        sp(ctx).edit().putString(KEY, out.toString()).apply()
    }

    @Synchronized
    fun all(ctx: Context): List<Record> {
        val arr = try { JSONArray(sp(ctx).getString(KEY, "[]")) } catch (_: Exception) { return emptyList() }
        val list = ArrayList<Record>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list.add(
                Record(
                    pkg = o.optString("p"), startedAt = o.optLong("s"), durationSec = o.optInt("d"),
                    avgFps = o.optInt("a"), low1 = o.optInt("l"), maxTemp = o.optInt("t"), batteryDrop = o.optInt("b"),
                    avgPing = o.optInt("ap"), spikes = o.optInt("sp"), stability = o.optInt("st"),
                )
            )
        }
        return list
    }

    fun summary(ctx: Context, pkg: String): Summary {
        val list = all(ctx).filter { it.pkg == pkg }
        if (list.isEmpty()) return Summary()
        val withFps = list.filter { it.avgFps > 0 }
        val withPing = list.filter { it.avgPing > 0 }
        val withStab = list.filter { it.stability > 0 }
        return Summary(
            sessions = list.size,
            totalSec = list.sumOf { it.durationSec.toLong() },
            avgFps = if (withFps.isEmpty()) 0 else withFps.map { it.avgFps }.average().toInt(),
            low1 = if (withFps.isEmpty()) 0 else withFps.map { it.low1 }.average().toInt(),
            maxTemp = list.maxOf { it.maxTemp },
            avgPing = if (withPing.isEmpty()) 0 else withPing.map { it.avgPing }.average().toInt(),
            spikes = list.sumOf { it.spikes },
            stability = if (withStab.isEmpty()) 0 else withStab.map { it.stability }.average().toInt(),
            last = list.maxByOrNull { it.startedAt },
        )
    }
}
