package com.siroha.gamespace.core.di

import com.siroha.gamespace.data.game.GameRepository
import com.siroha.gamespace.data.game.GameRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class GameModule {

    @Binds
    abstract fun bindGameRepository(impl: GameRepositoryImpl): GameRepository
}
