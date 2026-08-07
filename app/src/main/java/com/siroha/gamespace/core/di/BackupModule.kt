package com.siroha.gamespace.core.di

import com.siroha.gamespace.data.backup.BackupRepository
import com.siroha.gamespace.data.backup.BackupRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
