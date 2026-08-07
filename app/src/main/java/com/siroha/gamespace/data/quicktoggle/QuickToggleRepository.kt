package com.siroha.gamespace.data.quicktoggle

/** [Toggled] = actually flipped via the privilege layer. [OpenedSettings]
 *  = no elevated access, so a settings screen/panel was opened for the
 *  user to flip it themselves instead — Android has not allowed apps to
 *  directly toggle WiFi since API 29, or Bluetooth since API 33, without
 *  elevated access. */
sealed interface QuickToggleOutcome {
    data object Toggled : QuickToggleOutcome
    data object OpenedSettings : QuickToggleOutcome
}

interface QuickToggleRepository {
    suspend fun toggleWifi(): QuickToggleOutcome
    suspend fun toggleBluetooth(): QuickToggleOutcome

    /** Rotation lock lives in Settings.System, so — unlike WiFi/Bluetooth —
     *  it's reachable with the same WRITE_SETTINGS permission Booster's
     *  brightness control already uses, no root/Shizuku needed. */
    fun toggleRotationLock(): QuickToggleOutcome
    fun isRotationLocked(): Boolean
}
