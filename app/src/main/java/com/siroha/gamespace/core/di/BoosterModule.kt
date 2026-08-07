package com.siroha.gamespace.core.di

import com.siroha.gamespace.data.booster.BoosterRepository
import com.siroha.gamespace.data.booster.BoosterRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BoosterModule {

    @Binds
    abstract fun bindBoosterRepository(impl: BoosterRepositoryImpl): BoosterRepository
}
