package com.micrantha.eyespie.core

interface ThingProgressRepository {
    suspend fun get(gameId: GameId, thingId: ThingId, playerId: PlayerId): ThingProgress?
    suspend fun list(gameId: GameId, playerId: PlayerId): List<ThingProgress>
    suspend fun save(progress: ThingProgress)
}
