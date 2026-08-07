package com.siroha.gamespace.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    /** The visible library — excludes dismissed rows. */
    @Query("SELECT * FROM games WHERE isDismissed = 0")
    fun observeAll(): Flow<List<GameEntity>>

    /** Every package this table has ever seen, dismissed or not — what
     *  rescan() checks against so it never revives a dismissal. */
    @Query("SELECT packageName FROM games")
    suspend fun allKnownPackageNames(): List<String>

    /** Full rows, dismissed included — for JSON export, which should
     *  back up the whole table, not just what's currently visible. */
    @Query("SELECT * FROM games")
    suspend fun getAllIncludingDismissed(): List<GameEntity>

    @Query("SELECT * FROM games WHERE packageName = :packageName")
    suspend fun find(packageName: String): GameEntity?

    @Upsert
    suspend fun upsert(entity: GameEntity)

    @Query("UPDATE games SET isDismissed = 1 WHERE packageName = :packageName")
    suspend fun dismiss(packageName: String)

    @Query("UPDATE games SET isFavorite = :isFavorite WHERE packageName = :packageName")
    suspend fun setFavorite(packageName: String, isFavorite: Boolean)
}
