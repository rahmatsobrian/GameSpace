package com.siroha.gamespace.data.booster

interface BoosterRepository {

    // Brightness — WRITE_SETTINGS tier
    fun hasBrightnessPermission(): Boolean
    fun requestBrightnessPermission()
    /** 0-100. Reading Settings.System needs no special permission; only writing does. */
    fun getBrightness(): Int
    fun setBrightness(percent: Int)

    // Do Not Disturb — Notification Policy Access tier
    fun hasDndPermission(): Boolean
    fun requestDndPermission()
    fun isDndEnabled(): Boolean
    fun setDndEnabled(enabled: Boolean)

    // Animation scale + refresh rate — root/Shizuku tier, via
    // PrivilegeRepository.execPrivileged. These live in the Settings
    // Global/Secure tables, which — per current community-verified
    // practice, not just theory — need elevated shell access to write,
    // not the app-grantable WRITE_SETTINGS above. See ROADMAP.
    suspend fun setAnimationScale(scale: Float): Boolean
    /** Null resets to the device default (re-enables auto/adaptive
     *  switching) rather than locking to a specific rate. */
    suspend fun setRefreshRate(hz: Int?): Boolean
}
