package com.siroha.gamespace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * What we persist per game is membership + user intent, not display data —
 * name/icon are resolved live from PackageManager every time (see
 * InstalledAppsScanner) so this table can never drift out of sync with an
 * app update that changes its label or icon.
 */
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val packageName: String,
    val isFavorite: Boolean = false,
    /** false = found by the CATEGORY_GAME auto-scan; true = user added it
     *  explicitly (the correction path for games the heuristic misses). */
    val isManuallyAdded: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    /** Soft-delete. A hard DELETE would make rescan() unable to tell
     *  "never seen this package" from "user removed it" — since both look
     *  identical as a missing row — and would end up silently re-adding
     *  anything the user took out of their library. The row stays;
     *  [isDismissed] is what "not in the library" actually means. */
    val isDismissed: Boolean = false
)
