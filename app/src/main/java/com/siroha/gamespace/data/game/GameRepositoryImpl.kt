package com.siroha.gamespace.data.game

import android.content.Intent
import com.siroha.gamespace.core.usage.PlaySnapshot
import com.siroha.gamespace.core.usage.UsageAccessSource
import com.siroha.gamespace.data.local.GameDao
import com.siroha.gamespace.data.local.GameEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepositoryImpl @Inject constructor(
    private val gameDao: GameDao,
    private val scanner: InstalledAppsScanner,
    private val usageAccessSource: UsageAccessSource
) : GameRepository {

    override val library: Flow<List<Game>> = gameDao.observeAll().map { entities ->
        val usageByPackage = usageAccessSource.snapshotForAll()
        entities
            .filter { scanner.isInstalled(it.packageName) }
            .map { entity -> entity.toDomain(usageByPackage[entity.packageName]) }
            .sortedWith(compareByDescending<Game> { it.isFavorite }.thenBy { it.displayName.lowercase() })
    }

    override suspend fun rescan() {
        val alreadyKnown = gameDao.allKnownPackageNames().toSet()
        scanner.scanLaunchableApps()
            .filter { it.looksLikeGame && it.packageName !in alreadyKnown }
            .forEach { app ->
                gameDao.upsert(GameEntity(packageName = app.packageName, isManuallyAdded = false))
            }
    }

    override suspend fun addManually(packageName: String) {
        // A fresh upsert (isDismissed defaults to false) also correctly
        // "revives" a package the user had previously dismissed — unlike
        // rescan(), this is an explicit user action, so reviving it here
        // is the right call, not a bug.
        gameDao.upsert(GameEntity(packageName = packageName, isManuallyAdded = true))
    }

    override suspend fun remove(packageName: String) = gameDao.dismiss(packageName)

    override suspend fun setFavorite(packageName: String, isFavorite: Boolean) =
        gameDao.setFavorite(packageName, isFavorite)

    override suspend fun pickableApps(): List<InstalledAppInfo> {
        val inLibrary = gameDao.observeAll().first().map { it.packageName }.toSet()
        return scanner.scanLaunchableApps().filterNot { it.packageName in inLibrary }
    }

    override fun launchIntentFor(packageName: String): Intent? = scanner.launchIntentFor(packageName)

    private fun GameEntity.toDomain(snapshot: PlaySnapshot?): Game = Game(
        packageName = packageName,
        displayName = scanner.resolveLabel(packageName) ?: packageName,
        isFavorite = isFavorite,
        isManuallyAdded = isManuallyAdded,
        addedAt = addedAt,
        lastPlayedAt = snapshot?.lastTimeUsed,
        totalPlaytimeMillis = snapshot?.totalForegroundMillis ?: 0L
    )
}
