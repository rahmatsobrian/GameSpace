package com.siroha.gamespace.core.di

import com.siroha.gamespace.core.privilege.PrivilegeRepository
import com.siroha.gamespace.core.privilege.PrivilegeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PrivilegeModule {

    @Binds
    abstract fun bindPrivilegeRepository(
        impl: PrivilegeRepositoryImpl
    ): PrivilegeRepository
}
