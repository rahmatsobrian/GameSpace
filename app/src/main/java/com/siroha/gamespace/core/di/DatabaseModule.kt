package com.siroha.gamespace.core.di

import android.content.Context
import androidx.room.Room
import com.siroha.gamespace.data.local.AppDatabase
import com.siroha.gamespace.data.local.GameDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "gamespace.db").build()

    @Provides
    fun provideGameDao(database: AppDatabase): GameDao = database.gameDao()
}
