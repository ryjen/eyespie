package com.micrantha.eyespie.game

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId

/**
 * Read-only access to locally-cached display thumbnails for a game's targets.
 *
 * This is a narrow capability so features can show cached target photos for UX
 * without depending on SQLDelight rows. The cache is device-local only: it is
 * empty for imported games and never participates in matching or bundling.
 */
interface GameThumbnailCache {
    suspend fun thumbnailsForGame(gameId: GameId): Map<ThingId, ByteArray>
}
