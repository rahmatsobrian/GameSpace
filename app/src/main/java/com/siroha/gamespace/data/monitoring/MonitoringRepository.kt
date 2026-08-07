package com.siroha.gamespace.data.monitoring

import kotlinx.coroutines.flow.Flow

interface MonitoringRepository {
    /** Emits a fresh [DeviceSnapshot] every [intervalMillis] for as long as
     *  the returned Flow is collected — cancel collection (e.g. the
     *  screen leaving composition) to stop polling. */
    fun observe(intervalMillis: Long = 1_500L): Flow<DeviceSnapshot>
}
