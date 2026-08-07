package com.siroha.gamespace.core.di

import com.siroha.gamespace.data.quicktoggle.QuickToggleRepository
import com.siroha.gamespace.data.quicktoggle.QuickToggleRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class QuickToggleModule {

    @Binds
    abstract fun bindQuickToggleRepository(impl: QuickToggleRepositoryImpl): QuickToggleRepository
}
