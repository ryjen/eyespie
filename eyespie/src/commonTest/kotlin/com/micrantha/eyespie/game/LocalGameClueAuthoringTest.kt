package com.micrantha.eyespie.game

import com.micrantha.eyespie.core.Game
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.GameRepository
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.PlayerIdentityRepository
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.core.ThingProgress
import com.micrantha.eyespie.core.ThingProgressRepository
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_DIMENSIONS
import com.micrantha.eyespie.imaging.ImageEmbeddingGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class LocalGameClueAuthoringTest {
    @Test
    fun local_creator_appends_clue_without_replacing_existing_game_state() = runTest {
        val creator = PlayerIdentity(PlayerId("creator"), "Creator")
        val gameId = GameId("game-1")
        val games = RecordingGameRepository(
            Game(gameId, "Trip", creator.id),
        )
        val embeddings = RecordingEmbeddingGenerator()
        val loop = loop(creator, games, embeddings)

        val result = loop.addClue(
            gameId = gameId,
            clueText = "Find the red door",
            expectedAnswer = "red door",
            targetImage = CapturedImage.fromEncoded(byteArrayOf(1)),
        )

        val created = assertIs<LocalGameResult.Success<CreatedClue>>(result).value
        assertEquals(gameId, created.gameId)
        assertEquals("Find the red door", created.clue.clueText)
        assertEquals(1, games.game!!.things.size)
        assertEquals("red door", games.game!!.things.single().clueAuthority.expectedAnswer)
        assertEquals(1, embeddings.calls)
        assertEquals(1, games.saveCalls)
    }

    @Test
    fun non_creator_cannot_append_clue_and_embedding_is_not_run() = runTest {
        val creator = PlayerId("creator")
        val gameId = GameId("game-1")
        val games = RecordingGameRepository(Game(gameId, "Trip", creator))
        val embeddings = RecordingEmbeddingGenerator()
        val loop = loop(PlayerIdentity(PlayerId("other"), "Other"), games, embeddings)

        val result = loop.addClue(
            gameId = gameId,
            clueText = "Find it",
            expectedAnswer = "it",
            targetImage = CapturedImage.fromEncoded(byteArrayOf(1)),
        )

        val failure = assertIs<LocalGameResult.Failure>(result)
        assertEquals(LocalGameFailureCode.NOT_LOCAL_CREATOR, failure.failure.code)
        assertEquals(0, embeddings.calls)
        assertEquals(0, games.saveCalls)
        assertEquals(0, games.game!!.things.size)
    }

    private fun loop(
        identity: PlayerIdentity,
        games: GameRepository,
        embeddings: ImageEmbeddingGenerator,
    ) = LocalGameLoop(
        identityRepository = object : PlayerIdentityRepository {
            override suspend fun current(): PlayerIdentity = identity
        },
        gameRepository = games,
        progressRepository = object : ThingProgressRepository {
            override suspend fun get(gameId: GameId, thingId: ThingId, playerId: PlayerId): ThingProgress? = null
            override suspend fun list(gameId: GameId, playerId: PlayerId): List<ThingProgress> = emptyList()
            override suspend fun save(progress: ThingProgress) = Unit
        },
        embeddingGenerator = embeddings,
        idGenerator = object : LocalGameIdGenerator {
            override fun nextGameId(): GameId = GameId("unused")
            override fun nextThingId(): ThingId = ThingId("thing-1")
        },
    )
}

private class RecordingGameRepository(initial: Game) : GameRepository {
    var game: Game? = initial
    var saveCalls = 0

    override suspend fun list(): List<Game> = listOfNotNull(game)
    override suspend fun get(id: GameId): Game? = game?.takeIf { it.id == id }
    override suspend fun save(game: Game) {
        saveCalls += 1
        this.game = game
    }
}

private class RecordingEmbeddingGenerator : ImageEmbeddingGenerator {
    var calls = 0

    override suspend fun generate(image: CapturedImage): List<Float> {
        calls += 1
        return List(IMAGE_EMBEDDING_DIMENSIONS) { index -> if (index == 0) 1f else 0f }
    }
}
