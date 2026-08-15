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
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest

class LocalGameLoopTest {
    @Test
    fun createPersistsManualAuthorityButSnapshotExposesOnlyPlayableClue() = runTest {
        val games = InMemoryGameRepository()
        val progress = InMemoryProgressRepository()
        val embeddings = QueueEmbeddingGenerator(mutableListOf(unitVector(0)))
        val loop = loop(games = games, progress = progress, embeddings = embeddings)

        val created = success(
            loop.createGame(
                name = "  Road   Trip  ",
                clueText = "  Something   striped ",
                expectedAnswer = " pedestrian crossing ",
                targetImage = image(),
            ),
        )

        assertEquals("Road Trip", created.name)
        assertEquals("Something striped", created.clue.clueText)

        val stored = games.get(created.gameId)!!
        val authority = stored.things.single().clueAuthority
        assertEquals("pedestrian crossing", authority.expectedAnswer)
        assertEquals("Something striped", authority.playable().clueText)

        val snapshot = success(loop.loadSnapshot())
        val playable = snapshot.games.single().things.single()
        assertEquals("Something striped", playable.clue.clueText)
        assertNull(playable.progress)
    }

    @Test
    fun invalidClueFailsBeforeEmbeddingOrPersistence() = runTest {
        val games = InMemoryGameRepository()
        val embeddings = QueueEmbeddingGenerator(mutableListOf(unitVector(0)))
        val loop = loop(games = games, embeddings = embeddings)

        val result = loop.createGame(
            name = "Trip",
            clueText = "   ",
            expectedAnswer = "answer",
            targetImage = image(),
        )

        val failure = assertIs<LocalGameResult.Failure>(result)
        assertEquals(LocalGameFailureCode.INVALID_CLUE, failure.failure.code)
        assertEquals(0, embeddings.calls)
        assertEquals(0, games.saveCalls)
    }

    @Test
    fun targetEmbeddingFailureLeavesNoPartialGame() = runTest {
        val games = InMemoryGameRepository()
        val embeddings = QueueEmbeddingGenerator(
            outputs = mutableListOf(),
            failure = IllegalStateException("model unavailable"),
        )
        val loop = loop(games = games, embeddings = embeddings)

        val result = loop.createGame(
            name = "Trip",
            clueText = "Striped",
            expectedAnswer = "crosswalk",
            targetImage = image(),
        )

        assertEquals(
            LocalGameFailureCode.TARGET_EMBEDDING_FAILED,
            assertIs<LocalGameResult.Failure>(result).failure.code,
        )
        assertEquals(0, games.saveCalls)
        assertTrue(games.values.isEmpty())
    }

    @Test
    fun guessPersistsBestSimilarityAndMatchedStateAcrossLaterMiss() = runTest {
        val gameId = GameId("game-1")
        val thingId = ThingId("thing-1")
        val player = PlayerIdentity(PlayerId("player-1"), "Agent")
        val games = InMemoryGameRepository()
        val progress = InMemoryProgressRepository()
        val authority = accepted(ClueAuthority.manual("Striped", "crosswalk"))
        games.save(
            Game(
                id = gameId,
                name = "Trip",
                creator = player.id,
                things = listOf(
                    Thing(
                        id = thingId,
                        clueAuthority = authority,
                        targetEmbedding = unitVector(0),
                        matchThreshold = 0.75,
                    ),
                ),
            ),
        )
        val embeddings = QueueEmbeddingGenerator(
            mutableListOf(
                unitVector(0),
                unitVector(1),
            ),
        )
        val loop = loop(
            identity = player,
            games = games,
            progress = progress,
            embeddings = embeddings,
        )

        val first = success(loop.guess(gameId, thingId, image()))
        assertTrue(first.match.matched)
        assertEquals(1.0, first.progress.bestSimilarity)
        assertTrue(first.progress.matched)

        val second = success(loop.guess(gameId, thingId, image()))
        assertFalse(second.match.matched)
        assertEquals(1.0, second.progress.bestSimilarity)
        assertTrue(second.progress.matched)

        val snapshot = success(loop.loadSnapshot())
        val restored = snapshot.games.single().things.single().progress!!
        assertTrue(restored.matched)
        assertEquals(1.0, restored.bestSimilarity)
    }

    @Test
    fun cancellationPropagatesInsteadOfBecomingApplicationFailure() = runTest {
        val loop = loop(
            embeddings = QueueEmbeddingGenerator(
                outputs = mutableListOf(),
                failure = CancellationException("cancelled"),
            ),
        )

        assertFailsWith<CancellationException> {
            loop.createGame(
                name = "Trip",
                clueText = "Striped",
                expectedAnswer = "crosswalk",
                targetImage = image(),
            )
        }
    }

    @Test
    fun fatalErrorsPropagateInsteadOfBecomingRecoverableApplicationFailures() = runTest {
        val loop = loop(
            embeddings = QueueEmbeddingGenerator(
                outputs = mutableListOf(),
                failure = AssertionError("fatal runtime failure"),
            ),
        )

        assertFailsWith<AssertionError> {
            loop.createGame(
                name = "Trip",
                clueText = "Striped",
                expectedAnswer = "crosswalk",
                targetImage = image(),
            )
        }
    }

    @Test
    fun overlappingOperationFailsClosedInsteadOfQueueing() = runTest {
        val blocking = BlockingEmbeddingGenerator()
        val loop = loop(embeddings = blocking)

        val first = async {
            loop.createGame(
                name = "Trip",
                clueText = "Striped",
                expectedAnswer = "crosswalk",
                targetImage = image(),
            )
        }
        blocking.started.await()

        val overlapping = loop.createGame(
            name = "Other",
            clueText = "Round",
            expectedAnswer = "sign",
            targetImage = image(2),
        )
        assertEquals(
            LocalGameFailureCode.OPERATION_IN_PROGRESS,
            assertIs<LocalGameResult.Failure>(overlapping).failure.code,
        )

        blocking.release.complete(unitVector(0))
        assertTrue(first.await() is LocalGameResult.Success)
    }

    private fun loop(
        identity: PlayerIdentity = PlayerIdentity(PlayerId("player-1"), "Agent"),
        games: InMemoryGameRepository = InMemoryGameRepository(),
        progress: InMemoryProgressRepository = InMemoryProgressRepository(),
        embeddings: ImageEmbeddingGenerator = QueueEmbeddingGenerator(mutableListOf(unitVector(0))),
    ): LocalGameLoop = LocalGameLoop(
        identityRepository = FixedIdentityRepository(identity),
        gameRepository = games,
        progressRepository = progress,
        embeddingGenerator = embeddings,
        idGenerator = SequentialIdGenerator(),
    )

    private fun image(seed: Int = 1): CapturedImage =
        CapturedImage.fromEncoded(byteArrayOf(seed.toByte()))

    private fun unitVector(index: Int): List<Float> =
        List(IMAGE_EMBEDDING_DIMENSIONS) { if (it == index) 1f else 0f }

    private fun accepted(result: ClueAuthoringResult): ClueAuthority = when (result) {
        is ClueAuthoringResult.Accepted -> result.authority
        is ClueAuthoringResult.Rejected -> error("expected accepted clue authority")
    }

    private fun <T> success(result: LocalGameResult<T>): T = when (result) {
        is LocalGameResult.Success -> result.value
        is LocalGameResult.Failure -> error("expected success, got ${result.failure.code}")
    }
}

private class FixedIdentityRepository(
    private val identity: PlayerIdentity,
) : PlayerIdentityRepository {
    override suspend fun current(): PlayerIdentity = identity
}

private class SequentialIdGenerator : LocalGameIdGenerator {
    private var game = 0
    private var thing = 0

    override fun nextGameId(): GameId = GameId("game-${++game}")
    override fun nextThingId(): ThingId = ThingId("thing-${++thing}")
}

private class QueueEmbeddingGenerator(
    private val outputs: MutableList<List<Float>>,
    private val failure: Throwable? = null,
) : ImageEmbeddingGenerator {
    var calls: Int = 0
        private set

    override suspend fun generate(image: CapturedImage): List<Float> {
        calls += 1
        failure?.let { throw it }
        check(outputs.isNotEmpty()) { "no embedding fixture remains" }
        return outputs.removeAt(0)
    }
}

private class BlockingEmbeddingGenerator : ImageEmbeddingGenerator {
    val started = CompletableDeferred<Unit>()
    val release = CompletableDeferred<List<Float>>()

    override suspend fun generate(image: CapturedImage): List<Float> {
        started.complete(Unit)
        return release.await()
    }
}

private class InMemoryGameRepository : GameRepository {
    val values = linkedMapOf<GameId, Game>()
    var saveCalls: Int = 0
        private set

    override suspend fun list(): List<Game> = values.values.toList()

    override suspend fun get(id: GameId): Game? = values[id]

    override suspend fun save(game: Game) {
        saveCalls += 1
        values[game.id] = game
    }
}

private class InMemoryProgressRepository : ThingProgressRepository {
    private data class Key(
        val gameId: GameId,
        val thingId: ThingId,
        val playerId: PlayerId,
    )

    private val values = linkedMapOf<Key, ThingProgress>()

    override suspend fun get(
        gameId: GameId,
        thingId: ThingId,
        playerId: PlayerId,
    ): ThingProgress? = values[Key(gameId, thingId, playerId)]

    override suspend fun list(gameId: GameId, playerId: PlayerId): List<ThingProgress> =
        values.values.filter { it.gameId == gameId && it.playerId == playerId }

    override suspend fun save(progress: ThingProgress) {
        values[Key(progress.gameId, progress.thingId, progress.playerId)] = progress
    }
}
