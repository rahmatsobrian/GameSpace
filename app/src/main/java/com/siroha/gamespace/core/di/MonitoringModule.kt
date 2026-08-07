package com.siroha.gamespace.core.di

import com.siroha.gamespace.data.monitoring.MonitoringRepository
import com.siroha.gamespace.data.monitoring.MonitoringRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MonitoringModule {

    @Binds
    abstract fun bindMonitoringRepository(impl: MonitoringRepositoryImpl): MonitoringRepository
}
