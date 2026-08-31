package com.micrantha.eyespie.persistence

import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.core.Game
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.GameRepository
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.Thing
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.data.EyesPieDatabase
import com.micrantha.eyespie.game.GameThumbnailCache

class SqlGameRepository(
    private val database: EyesPieDatabase,
) : GameRepository, GameThumbnailCache {
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
                val thumbnail = thing.targetThumbnail
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
                    thumbnail,
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
                    thumbnail,
                    thing.id.value,
                )
            }
        }
    }

    override suspend fun thumbnailsForGame(gameId: GameId): Map<ThingId, ByteArray> =
        queries.selectThingThumbnailsByGame(gameId.value) { thingId, thumbnail ->
            ThingId(thingId) to (thumbnail?.copyOf() ?: byteArrayOf())
        }.executeAsList()
            .filter { (_, bytes) -> bytes.isNotEmpty() }
            .toMap()

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
                _,
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
