package com.micrantha.eyespie.persistence

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.core.ThingProgress
import com.micrantha.eyespie.core.ThingProgressRepository
import com.micrantha.eyespie.data.EyesPieDatabase

class SqlThingProgressRepository(
    database: EyesPieDatabase,
) : ThingProgressRepository {
    private val queries = database.eyesPieQueries

    override suspend fun get(
        gameId: GameId,
        thingId: ThingId,
        playerId: PlayerId,
    ): ThingProgress? = queries.selectThingProgress(
        gameId.value,
        thingId.value,
        playerId.value,
    ) { persistedGameId, persistedThingId, persistedPlayerId, matched, bestSimilarity ->
        mapProgress(persistedGameId, persistedThingId, persistedPlayerId, matched, bestSimilarity)
    }.executeAsOneOrNull()

    override suspend fun list(gameId: GameId, playerId: PlayerId): List<ThingProgress> =
        queries.selectProgressByGameAndPlayer(gameId.value, playerId.value) {
                persistedGameId,
                persistedThingId,
                persistedPlayerId,
                matched,
                bestSimilarity,
            ->
            mapProgress(persistedGameId, persistedThingId, persistedPlayerId, matched, bestSimilarity)
        }.executeAsList()

    override suspend fun save(progress: ThingProgress) {
        queries.upsertThingProgress(
            progress.gameId.value,
            progress.thingId.value,
            progress.playerId.value,
            if (progress.matched) 1L else 0L,
            progress.bestSimilarity,
        )
    }

    private fun mapProgress(
        gameId: String,
        thingId: String,
        playerId: String,
        matched: Long,
        bestSimilarity: Double?,
    ): ThingProgress = ThingProgress(
        gameId = GameId(gameId),
        thingId = ThingId(thingId),
        playerId = PlayerId(playerId),
        matched = matched != 0L,
        bestSimilarity = bestSimilarity,
    )
}
