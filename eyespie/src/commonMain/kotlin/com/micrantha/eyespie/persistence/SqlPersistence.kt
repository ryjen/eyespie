package com.micrantha.eyespie.persistence

import com.micrantha.eyespie.core.Game
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.GameRepository
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.Thing
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.core.ThingProgress
import com.micrantha.eyespie.core.ThingProgressRepository
import com.micrantha.eyespie.data.EyesPieDatabase

interface EyespieDatabaseFactory {
    fun create(): EyesPieDatabase
}

class SqlGameRepository(
    private val database: EyesPieDatabase,
) : GameRepository {
    private val queries = database.eyesPieQueries

    override suspend fun list(): List<Game> = queries.selectAllGames { id, name, creatorId ->
        loadGame(id, name, creatorId)
    }.executeAsList()

    override suspend fun get(id: GameId): Game? = queries.selectGameById(id.value) { gameId, name, creatorId ->
        loadGame(gameId, name, creatorId)
    }.executeAsOneOrNull()

    override suspend fun save(game: Game) {
        queries.transaction {
            queries.upsertGame(game.id.value, game.name, game.creator.value)

            val retainedThingIds = game.things.mapTo(mutableSetOf()) { it.id.value }
            queries.selectThingIdsByGame(game.id.value)
                .executeAsList()
                .filterNot(retainedThingIds::contains)
                .forEach { staleThingId ->
                    queries.deleteProgressForThing(staleThingId)
                    queries.deleteThing(staleThingId)
                }

            game.things.forEachIndexed { index, thing ->
                queries.upsertThing(
                    thing.id.value,
                    game.id.value,
                    thing.clue,
                    EmbeddingBlobCodec.encode(thing.targetEmbedding),
                    thing.matchThreshold,
                    index.toLong(),
                )
            }
        }
    }

    private fun loadGame(id: String, name: String, creatorId: String): Game = Game(
        id = GameId(id),
        name = name,
        creator = PlayerId(creatorId),
        things = queries.selectThingsByGame(id) { thingId, _, clue, targetEmbedding, matchThreshold, _ ->
            Thing(
                id = ThingId(thingId),
                clue = clue,
                targetEmbedding = EmbeddingBlobCodec.decode(targetEmbedding),
                matchThreshold = matchThreshold,
            )
        }.executeAsList(),
    )
}

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
