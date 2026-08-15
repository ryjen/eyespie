package com.micrantha.eyespie.persistence

import com.micrantha.eyespie.clue.ClueAuthority
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
        GameRow(id, name, creatorId)
    }.executeAsList().map(::loadGame)

    override suspend fun get(id: GameId): Game? = queries.selectGameById(id.value) { gameId, name, creatorId ->
        GameRow(gameId, name, creatorId)
    }.executeAsOneOrNull()?.let(::loadGame)

    override suspend fun save(game: Game) {
        queries.transaction {
            queries.insertGame(game.id.value, game.name, game.creator.value)
            queries.updateGame(game.name, game.creator.value, game.id.value)

            val retainedThingIds = game.things.mapTo(mutableSetOf()) { it.id.value }
            queries.selectThingIdsByGame(game.id.value)
                .executeAsList()
                .filterNot(retainedThingIds::contains)
                .forEach { staleThingId ->
                    queries.deleteProgressForThing(staleThingId)
                    queries.deleteThing(staleThingId)
                }

            game.things.forEachIndexed { index, thing ->
                val embedding = EmbeddingBlobCodec.encode(thing.targetEmbedding)
                val sortOrder = index.toLong()
                val clueAuthority = thing.clueAuthority
                val generatedProvenance = clueAuthority.generatedProvenance
                queries.insertThing(
                    thing.id.value,
                    game.id.value,
                    clueAuthority.clueText,
                    embedding,
                    thing.matchThreshold,
                    sortOrder,
                    clueAuthority.expectedAnswer,
                    clueAuthority.origin.name,
                    clueAuthority.schemaVersion.toLong(),
                    generatedProvenance?.providerId,
                    generatedProvenance?.modelId,
                    generatedProvenance?.confidence,
                )
                queries.updateThing(
                    game.id.value,
                    clueAuthority.clueText,
                    embedding,
                    thing.matchThreshold,
                    sortOrder,
                    clueAuthority.expectedAnswer,
                    clueAuthority.origin.name,
                    clueAuthority.schemaVersion.toLong(),
                    generatedProvenance?.providerId,
                    generatedProvenance?.modelId,
                    generatedProvenance?.confidence,
                    thing.id.value,
                )
            }
        }
    }

    private fun loadGame(row: GameRow): Game = Game(
        id = GameId(row.id),
        name = row.name,
        creator = PlayerId(row.creatorId),
        things = queries.selectThingsByGame(row.id) {
                thingId,
                _,
                clue,
                targetEmbedding,
                matchThreshold,
                _,
                expectedAnswer,
                clueOrigin,
                clueAuthorityVersion,
                generatedProviderId,
                generatedModelId,
                generatedConfidence,
            ->
            Thing(
                id = ThingId(thingId),
                clueAuthority = ClueAuthority.persisted(
                    schemaVersion = clueAuthorityVersion.toInt(),
                    clueText = clue,
                    expectedAnswer = expectedAnswer,
                    origin = clueOrigin,
                    generatedProviderId = generatedProviderId,
                    generatedModelId = generatedModelId,
                    generatedConfidence = generatedConfidence,
                ),
                targetEmbedding = EmbeddingBlobCodec.decode(targetEmbedding),
                matchThreshold = matchThreshold,
            )
        }.executeAsList(),
    )

    private data class GameRow(
        val id: String,
        val name: String,
        val creatorId: String,
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
