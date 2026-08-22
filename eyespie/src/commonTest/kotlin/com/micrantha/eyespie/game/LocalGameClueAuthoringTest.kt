package com.micrantha.eyespie.game

import com.micrantha.eyespie.clue.ClueAuthoringResult
import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.core.Game
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.GameRepository
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.PlayerIdentityRepository
import com.micrantha.eyespie.core.Thing
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.core.ThingProgress
import com.micrantha.eyespie.core.ThingProgressRepository
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_DIMENSIONS
import com.micrantha.eyespie.imaging.ImageEmbeddingGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class LocalGameClueAuthoringTest {
    private val localPlayer = PlayerIdentity(PlayerId("player-local"), "Agent")
    private val gameId = GameId("game-1")
    private val existingThing = Thing(
        id = ThingId("thing-1"),
        clueAuthority = accepted(ClueAuthority.manual("First clue", "first answer")),
        targetEmbedding = unitVector(0),
    )

    @Test
    fun local_creator_can_append_clue() = runTest {
        val original = game(localPlayer.id)
        val games = ClueGameRepository(original)
        val embeddings = CountingEmbeddingGenerator(unitVector(1))
        val loop = loop(games, embeddings)

        val result = loop.addClue(
            gameId = gameId,
            clueText = "Second clue",
            expectedAnswer = "second answer",
            targetImage = image(),
        )

        val authored = assertIs<LocalGameResult.Success<AuthoredThing>>(result).value
        assertEquals(gameId, authored.gameId)
        assertEquals("Second clue", authored.clue.clueText)
        assertEquals(1, embeddings.calls)
        assertEquals(2, games.current.things.size)
        assertEquals("Second clue", games.current.things.last().playableClue().clueText)
        assertEquals("second answer", games.current.things.last().clueAuthority.expectedAnswer)
    }

    @Test
    fun non_creator_fails_before_embedding_and_does_not_modify_game() = runTest {
        val original = game(PlayerId("different-creator"))
        val games = ClueGameRepository(original)
        val embeddings = CountingEmbeddingGenerator(unitVector(1))
        val loop = loop(games, embeddings)

        val result = loop.addClue(
            gameId = gameId,
            clueText = "Unauthorized clue",
            expectedAnswer = "answer",
            targetImage = image(),
        )

        assertEquals(
            LocalGameFailureCode.NOT_LOCAL_CREATOR,
            assertIs<LocalGameResult.Failure>(result).failure.code,
        )
        assertEquals(0, embeddings.calls)
        assertEquals(0, games.saveCalls)
        assertEquals(original, games.current)
    }

    @Test
    fun embedding_failure_leaves_existing_game_unchanged() = runTest {
        val original = game(localPlayer.id)
        val games = ClueGameRepository(original)
        val embeddings = CountingEmbeddingGenerator(
            embedding = unitVector(1),
            failure = IllegalStateException("model unavailable"),
        )
        val loop = loop(games, embeddings)

        val result = loop.addClue(
            gameId = gameId,
            clueText = "Second clue",
            expectedAnswer = "second answer",
            targetImage = image(),
        )

        assertEquals(
            LocalGameFailureCode.TARGET_EMBEDDING_FAILED,
            assertIs<LocalGameResult.Failure>(result).failure.code,
        )
        assertEquals(1, embeddings.calls)
        assertEquals(0, games.saveCalls)
        assertEquals(original, games.current)
    }

    @Test
    fun persistence_failure_leaves_repository_authority_unchanged() = runTest {
        val original = game(localPlayer.id)
        val games = ClueGameRepository(original, failSaves = true)
        val embeddings = CountingEmbeddingGenerator(unitVector(1))
        val loop = loop(games, embeddings)

        val result = loop.addClue(
            gameId = gameId,
            clueText = "Second clue",
            expectedAnswer = "second answer",
            targetImage = image(),
        )

        assertEquals(
            LocalGameFailureCode.PERSISTENCE_FAILED,
            assertIs<LocalGameResult.Failure>(result).failure.code,
        )
        assertEquals(1, embeddings.calls)
        assertEquals(1, games.saveCalls)
        assertEquals(original, games.current)
    }

    private fun game(creator: PlayerId): Game = Game(
        id = gameId,
        name = "Trip",
        creator = creator,
        things = listOf(existingThing),
    )

    private fun loop(
        games: GameRepository,
        embeddings: ImageEmbeddingGenerator,
    ): LocalGameLoop = LocalGameLoop(
        identityRepository = object : PlayerIdentityRepository {
            override suspend fun current(): PlayerIdentity = localPlayer
        },
        gameRepository = games,
        progressRepository = EmptyClueProgressRepository,
        embeddingGenerator = embeddings,
        idGenerator = object : LocalGameIdGenerator {
            override fun nextGameId(): GameId = GameId("unused")
            override fun nextThingId(): ThingId = ThingId("thing-2")
        },
    )

    private fun image(): CapturedImage = CapturedImage.fromEncoded(byteArrayOf(1))

    private fun unitVector(index: Int): List<Float> =
        List(IMAGE_EMBEDDING_DIMENSIONS) { if (it == index) 1f else 0f }

    private fun accepted(result: ClueAuthoringResult): ClueAuthority = when (result) {
        is ClueAuthoringResult.Accepted -> result.authority
        is ClueAuthoringResult.Rejected -> error("expected accepted clue authority")
    }
}

private class CountingEmbeddingGenerator(
    private val embedding: List<Float>,
    private val failure: Throwable? = null,
) : ImageEmbeddingGenerator {
    var calls = 0
        private set

    override suspend fun generate(image: CapturedImage): List<Float> {
        calls += 1
        failure?.let { throw it }
        return embedding
    }
}

private class ClueGameRepository(
    initial: Game,
    private val failSaves: Boolean = false,
) : GameRepository {
    var current: Game = initial
        private set
    var saveCalls = 0
        private set

    override suspend fun list(): List<Game> = listOf(current)

    override suspend fun get(id: GameId): Game? = current.takeIf { it.id == id }

    override suspend fun save(game: Game) {
        saveCalls += 1
        if (failSaves) error("persistence failed")
        current = game
    }
}

private object EmptyClueProgressRepository : ThingProgressRepository {
    override suspend fun get(
        gameId: GameId,
        thingId: ThingId,
        playerId: PlayerId,
    ): ThingProgress? = null

    override suspend fun list(gameId: GameId, playerId: PlayerId): List<ThingProgress> = emptyList()

    override suspend fun save(progress: ThingProgress) = Unit
}
