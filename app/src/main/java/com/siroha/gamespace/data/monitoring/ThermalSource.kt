package com.siroha.gamespace.data.monitoring

import android.content.Context
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun snapshot(): ThermalSnapshot {
        val status = mapStatus(powerManager.currentThermalStatus)

        // getThermalHeadroom needs a couple of calls before it has enough
        // history to forecast anything and can return NaN in the
        // meantime — that's not an error, just "not ready yet."
        val headroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            powerManager.getThermalHeadroom(0).takeUnless { it.isNaN() }
        } else {
            null
        }

        return ThermalSnapshot(status = status, headroom = headroom)
    }

    private fun mapStatus(value: Int): ThermalStatus = when (value) {
        PowerManager.THERMAL_STATUS_NONE -> ThermalStatus.NONE
        PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.LIGHT
        PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.MODERATE
        PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.SEVERE
        PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.CRITICAL
        PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.EMERGENCY
        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.SHUTDOWN
        else -> ThermalStatus.UNKNOWN
    }
}
