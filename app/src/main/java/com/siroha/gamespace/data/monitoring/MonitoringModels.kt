package com.siroha.gamespace.data.monitoring

data class BatterySnapshot(
    val percent: Int,
    val temperatureCelsius: Float,
    val isCharging: Boolean,
    val chargePlug: ChargePlug
)

enum class ChargePlug { AC, USB, WIRELESS, NONE }

data class RamSnapshot(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedPercent: Int,
    /** System-defined low-memory threshold, from the same MemoryInfo call —
     *  not a number this app picked. */
    val isLowMemory: Boolean
)

enum class ThermalStatus { NONE, LIGHT, MODERATE, SEVERE, CRITICAL, EMERGENCY, SHUTDOWN, UNKNOWN }

data class ThermalSnapshot(
    val status: ThermalStatus,
    /** 0.0 (no throttling) to 1.0 (severe-throttling threshold); can exceed
     *  1.0. Null below API 30 (getThermalHeadroom didn't exist yet) or if
     *  the device returned NaN (needs a couple of calls before it can
     *  forecast anything — see PowerManager docs). */
    val headroom: Float?
)

/**
 * Deliberately not "just a percentage." Real system CPU load has no public
 * API on Android 10-15 for a non-privileged app — see ROADMAP for why —
 * so a genuine number only exists once root or Shizuku is granted.
 * Pretending otherwise (e.g. estimating from something unrelated) would be
 * exactly the kind of fake-but-plausible-looking number this project is
 * trying not to ship.
 */
sealed interface CpuSnapshot {
    data class Percent(val value: Int) : CpuSnapshot
    data object Unavailable : CpuSnapshot
}

data class DeviceSnapshot(
    val battery: BatterySnapshot?,
    val ram: RamSnapshot,
    val thermal: ThermalSnapshot,
    val cpu: CpuSnapshot
)
