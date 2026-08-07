package com.siroha.gamespace.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GameEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
