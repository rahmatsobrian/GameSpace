package com.siroha.gamespace.data.monitoring

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoringRepositoryImpl @Inject constructor(
    private val batterySource: BatterySource,
    private val ramSource: RamSource,
    private val thermalSource: ThermalSource,
    private val cpuSource: CpuSource
) : MonitoringRepository {

    override fun observe(intervalMillis: Long): Flow<DeviceSnapshot> = flow {
        while (true) {
            emit(
                DeviceSnapshot(
                    battery = batterySource.snapshot(),
                    ram = ramSource.snapshot(),
                    thermal = thermalSource.snapshot(),
                    cpu = cpuSource.snapshot()
                )
            )
            delay(intervalMillis)
        }
    }.flowOn(Dispatchers.IO)
}
