package com.siroha.gamespace.data.backup

import android.content.Context
import android.net.Uri
import com.siroha.gamespace.data.local.GameDao
import com.siroha.gamespace.data.local.GameEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao
) : BackupRepository {

    override suspend fun exportLibrary(destination: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val entities = gameDao.getAllIncludingDismissed()
            val gamesArray = JSONArray()
            entities.forEach { entity -> gamesArray.put(entity.toJson()) }

            val root = JSONObject()
                .put("schemaVersion", 1)
                .put("exportedAt", System.currentTimeMillis())
                .put("games", gamesArray)

            val stream = context.contentResolver.openOutputStream(destination)
                ?: return@runCatching false
            stream.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
            true
        }.getOrElse { false }
    }

    override suspend fun importLibrary(source: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(source)?.use {
                it.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@runCatching false

            val root = JSONObject(text)
            val gamesArray = root.optJSONArray("games") ?: return@runCatching false
            for (i in 0 until gamesArray.length()) {
                gameDao.upsert(gamesArray.getJSONObject(i).toGameEntity())
            }
            true
        }.getOrElse { false }
    }

    private fun GameEntity.toJson(): JSONObject = JSONObject()
        .put("packageName", packageName)
        .put("isFavorite", isFavorite)
        .put("isManuallyAdded", isManuallyAdded)
        .put("addedAt", addedAt)
        .put("isDismissed", isDismissed)

    private fun JSONObject.toGameEntity(): GameEntity = GameEntity(
        packageName = getString("packageName"),
        isFavorite = optBoolean("isFavorite", false),
        isManuallyAdded = optBoolean("isManuallyAdded", false),
        addedAt = optLong("addedAt", System.currentTimeMillis()),
        isDismissed = optBoolean("isDismissed", false)
    )
}
