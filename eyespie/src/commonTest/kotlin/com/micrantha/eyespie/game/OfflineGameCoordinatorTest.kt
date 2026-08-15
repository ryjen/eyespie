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
import com.micrantha.eyespie.imaging.ImageEmbeddingGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OfflineGameCoordinatorTest {
    @Test
    fun createStoresAuthorityButReturnsOnlyPlayableProjection() = runTest {
        val games = MemoryGameRepository()
        val coordinator = coordinator(
            games = games,
            embeddings = QueueEmbeddingGenerator(axis(0)),
        )

        val created = assertIs<OfflineResult.Success<PlayableGameState>>(
            coordinator.createManualGame(
                ManualGameDraft(
                    name = "  Road   Trip  ",
                    clueText = "  Something   striped ",
                    expectedAnswer = "  crosswalk  ",
                ),
                image(),
            ),
        ).value

        assertEquals("Road Trip", created.name)
        assertEquals("Something striped", created.things.single().clue.clueText)
        assertFalse(created.toString().contains("crosswalk"))

        val persisted = games.list().single().things.single()
        assertEquals("crosswalk", persisted.clueAuthority.expectedAnswer)
        assertEquals(axis(0), persisted.targetEmbedding)
        assertEquals(PlayerId("player-1"), games.list().single().creator)
    }

    @Test
    fun guessPersistsStickyMatchAndMonotonicBestSimilarity() = runTest {
        val games = MemoryGameRepository()
        val progress = MemoryProgressRepository()
        val coordinator = coordinator(
            games = games,
            progress = progress,
            embeddings = QueueEmbeddingGenerator(
                axis(0),
                axis(1),
                axis(0),
                axis(1),
            ),
        )
        val created = assertIs<OfflineResult.Success<PlayableGameState>>(
            coordinator.createManualGame(draft(), image()),
        ).value
        val thingId = created.things.single().id

        val miss = assertIs<OfflineResult.Success<GuessOutcome>>(
            coordinator.guess(created.id, thingId, image()),
        ).value
        assertFalse(miss.matched)
        assertEquals(0.0, miss.similarity, 0.000001)

        val match = assertIs<OfflineResult.Success<GuessOutcome>>(
            coordinator.guess(created.id, thingId, image()),
        ).value
        assertTrue(match.matched)
        assertEquals(1.0, match.similarity, 0.000001)

        val laterMiss = assertIs<OfflineResult.Success<GuessOutcome>>(
            coordinator.guess(created.id, thingId, image()),
        ).value
        assertTrue(laterMiss.matched)
        assertEquals(1.0, laterMiss.game.things.single().bestSimilarity)

        val restored = assertIs<OfflineResult.Success<List<PlayableGameState>>>(
            coordinator.loadGames(),
        ).value.single()
        assertTrue(restored.things.single().matched)
        assertEquals(1.0, restored.things.single().bestSimilarity)
    }

    @Test
    fun invalidEmbeddingFailsClosedWithoutSavingAuthority() = runTest {
        val games = MemoryGameRepository()
        val coordinator = coordinator(
            games = games,
            embeddings = QueueEmbeddingGenerator(listOf(1f, 0f)),
        )

        val result = assertIs<OfflineResult.Failure>(
            coordinator.createManualGame(draft(), image()),
        )

        assertEquals(OfflineFailureCode.INVALID_EMBEDDING, result.failure.code)
        assertTrue(games.list().isEmpty())
    }

    @Test
    fun nonFiniteEmbeddingFailsClosedWithoutSavingAuthority() = runTest {
        val games = MemoryGameRepository()
        val invalid = axis(0).toMutableList().apply { this[10] = Float.NaN }
        val coordinator = coordinator(
            games = games,
            embeddings = QueueEmbeddingGenerator(invalid),
        )

        val result = assertIs<OfflineResult.Failure>(
            coordinator.createManualGame(draft(), image()),
        )

        assertEquals(OfflineFailureCode.INVALID_EMBEDDING, result.failure.code)
        assertTrue(games.list().isEmpty())
    }

    @Test
    fun persistenceFailureDoesNotPublishPartialGameState() = runTest {
        val games = MemoryGameRepository(failSave = true)
        val coordinator = coordinator(
            games = games,
            embeddings = QueueEmbeddingGenerator(axis(0)),
        )

        val result = assertIs<OfflineResult.Failure>(
            coordinator.createManualGame(draft(), image()),
        )

        assertEquals(OfflineFailureCode.PERSISTENCE_FAILED, result.failure.code)
        assertTrue(games.list().isEmpty())
    }

    @Test
    fun concurrentEmbeddingAttemptFailsBusyInsteadOfQueueing() = runTest {
        val gate = CompletableDeferred<Unit>()
        val entered = CompletableDeferred<Unit>()
        val generator = object : ImageEmbeddingGenerator {
            override suspend fun generate(image: CapturedImage): List<Float> {
                entered.complete(Unit)
                gate.await()
                return axis(0)
            }
        }
        val coordinator = coordinator(embeddings = generator)
        val first = async { coordinator.createManualGame(draft(), image()) }
        entered.await()

        val second = assertIs<OfflineResult.Failure>(
            coordinator.createManualGame(draft(), image()),
        )
        assertEquals(OfflineFailureCode.BUSY, second.failure.code)

        gate.complete(Unit)
        assertIs<OfflineResult.Success<PlayableGameState>>(first.await())
    }

    @Test
    fun cancellationRemainsCancellationAndDoesNotSave() = runTest {
        val games = MemoryGameRepository()
        val coordinator = coordinator(
            games = games,
            embeddings = object : ImageEmbeddingGenerator {
                override suspend fun generate(image: CapturedImage): List<Float> {
                    throw CancellationException("test cancellation")
                }
            },
        )

        assertFailsWith<CancellationException> {
            coordinator.createManualGame(draft(), image())
        }
        assertTrue(games.list().isEmpty())
    }

    private fun coordinator(
        games: MemoryGameRepository = MemoryGameRepository(),
        progress: MemoryProgressRepository = MemoryProgressRepository(),
        embeddings: ImageEmbeddingGenerator,
    ): OfflineGameCoordinator {
        val ids = ArrayDeque(listOf("game-id", "thing-id", "game-id-2", "thing-id-2"))
        return OfflineGameCoordinator(
            identityRepository = StaticIdentityRepository,
            gameRepository = games,
            progressRepository = progress,
            embeddingGenerator = embeddings,
            idGenerator = { ids.removeFirst() },
        )
    }

    private fun draft() = ManualGameDraft(
        name = "Road Trip",
        clueText = "Something striped",
        expectedAnswer = "crosswalk",
    )

    private fun image(): CapturedImage = CapturedImage.fromEncoded(byteArrayOf(1, 2, 3))

    private fun axis(index: Int): List<Float> = List(1024) { if (it == index) 1f else 0f }

    private object StaticIdentityRepository : PlayerIdentityRepository {
        override suspend fun current(): PlayerIdentity = PlayerIdentity(
            id = PlayerId("player-1"),
            displayName = "Agent",
        )
    }

    private class QueueEmbeddingGenerator(vararg embeddings: List<Float>) : ImageEmbeddingGenerator {
        private val queue = ArrayDeque(embeddings.toList())

        override suspend fun generate(image: CapturedImage): List<Float> = queue.removeFirst()
    }

    private class MemoryGameRepository(
        private val failSave: Boolean = false,
    ) : GameRepository {
        private val games = linkedMapOf<GameId, Game>()

        override suspend fun list(): List<Game> = games.values.toList()

        override suspend fun get(id: GameId): Game? = games[id]

        override suspend fun save(game: Game) {
            if (failSave) throw IllegalStateException("simulated persistence failure")
            games[game.id] = game
        }
    }

    private class MemoryProgressRepository : ThingProgressRepository {
        private val progress = linkedMapOf<Triple<GameId, ThingId, PlayerId>, ThingProgress>()

        override suspend fun get(
            gameId: GameId,
            thingId: ThingId,
            playerId: PlayerId,
        ): ThingProgress? = progress[Triple(gameId, thingId, playerId)]

        override suspend fun list(gameId: GameId, playerId: PlayerId): List<ThingProgress> =
            progress.values.filter { it.gameId == gameId && it.playerId == playerId }

        override suspend fun save(progress: ThingProgress) {
            this.progress[Triple(progress.gameId, progress.thingId, progress.playerId)] = progress
        }
    }
}
