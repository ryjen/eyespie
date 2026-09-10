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
import com.micrantha.eyespie.telemetry.DiagnosticCode
import com.micrantha.eyespie.telemetry.DiagnosticOperation
import com.micrantha.eyespie.telemetry.DiagnosticResult
import com.micrantha.eyespie.telemetry.DiagnosticSink
import com.micrantha.eyespie.telemetry.FakeDiagnosticSink
import com.micrantha.eyespie.telemetry.OperationalTelemetry
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class LocalGameLoopTelemetryTest {
    @Test
    fun successfulCreateEmitsStageAndOperationOutcomeMetadata() = runTest {
        val sink = FakeDiagnosticSink()
        val loop = loop(telemetry = OperationalTelemetry(sink))

        val result = loop.createGame(
            name = "Trip",
            clueText = "Striped",
            expectedAnswer = "crosswalk",
            targetImage = image(),
        )

        assertIs<LocalGameResult.Success<CreatedGame>>(result)
        assertEquals(
            listOf(
                DiagnosticOperation.TARGET_EMBEDDING_GENERATE,
                DiagnosticOperation.GAME_PERSIST,
                DiagnosticOperation.GAME_CREATE,
            ),
            sink.records.map { it.operation },
        )
        assertEquals(listOf(DiagnosticResult.SUCCESS, DiagnosticResult.SUCCESS, DiagnosticResult.SUCCESS), sink.records.map { it.result })
        assertEquals(listOf(null, null, null), sink.records.map { it.code })
    }

    @Test
    fun embeddingFailureMapsStageAndParentToStableDiagnostics() = runTest {
        val sink = FakeDiagnosticSink()
        val loop = loop(
            embeddingGenerator = TelemetryEmbeddingGenerator(
                failure = IllegalStateException("model unavailable with private implementation detail"),
            ),
            telemetry = OperationalTelemetry(sink),
        )

        val result = loop.createGame(
            name = "Trip",
            clueText = "Striped",
            expectedAnswer = "crosswalk",
            targetImage = image(),
        )

        val failure = assertIs<LocalGameResult.Failure>(result)
        assertEquals(LocalGameFailureCode.TARGET_EMBEDDING_FAILED, failure.failure.code)
        assertEquals(
            listOf(
                DiagnosticOperation.TARGET_EMBEDDING_GENERATE,
                DiagnosticOperation.GAME_CREATE,
            ),
            sink.records.map { it.operation },
        )
        assertEquals(DiagnosticCode.TARGET_EMBEDDING_FAILED, sink.records[0].code)
        assertEquals(DiagnosticCode.TARGET_EMBEDDING_FAILED, sink.records[1].code)
    }

    @Test
    fun cancellationIsRecordedAtStageAndParentButStillPropagates() = runTest {
        val sink = FakeDiagnosticSink()
        val loop = loop(
            embeddingGenerator = TelemetryEmbeddingGenerator(
                failure = CancellationException("cancelled"),
            ),
            telemetry = OperationalTelemetry(sink),
        )

        assertFailsWith<CancellationException> {
            loop.createGame(
                name = "Trip",
                clueText = "Striped",
                expectedAnswer = "crosswalk",
                targetImage = image(),
            )
        }

        assertEquals(
            listOf(
                DiagnosticOperation.TARGET_EMBEDDING_GENERATE,
                DiagnosticOperation.GAME_CREATE,
            ),
            sink.records.map { it.operation },
        )
        assertEquals(listOf(DiagnosticResult.CANCELLED, DiagnosticResult.CANCELLED), sink.records.map { it.result })
        assertEquals(listOf(null, null), sink.records.map { it.code })
    }

    @Test
    fun guessEmbeddingFailureIsNotMisclassifiedAsMatchPolicyFailure() = runTest {
        val sink = FakeDiagnosticSink()
        val generator = FailAfterFirstEmbeddingGenerator()
        val loop = loop(
            embeddingGenerator = generator,
            telemetry = OperationalTelemetry(sink),
        )

        val created = assertIs<LocalGameResult.Success<CreatedGame>>(
            loop.createGame(
                name = "Trip",
                clueText = "Striped",
                expectedAnswer = "crosswalk",
                targetImage = image(),
            ),
        ).value

        val result = loop.guess(
            gameId = created.gameId,
            thingId = created.thingId,
            guessImage = image(),
        )

        val failure = assertIs<LocalGameResult.Failure>(result)
        assertEquals(LocalGameFailureCode.GUESS_EMBEDDING_FAILED, failure.failure.code)
        assertEquals(DiagnosticOperation.GUESS_EMBEDDING_GENERATE, sink.records[sink.records.lastIndex - 1].operation)
        assertEquals(DiagnosticCode.GUESS_EMBEDDING_FAILED, sink.records[sink.records.lastIndex - 1].code)
        assertEquals(DiagnosticOperation.GAME_GUESS, sink.records.last().operation)
        assertEquals(DiagnosticCode.GUESS_EMBEDDING_FAILED, sink.records.last().code)
    }

    @Test
    fun diagnosticStorageFailureCannotFailSuccessfulGameplay() = runTest {
        val loop = loop(
            telemetry = OperationalTelemetry(
                DiagnosticSink { throw IllegalStateException("diagnostic sink unavailable") },
            ),
        )

        val result = loop.createGame(
            name = "Trip",
            clueText = "Striped",
            expectedAnswer = "crosswalk",
            targetImage = image(),
        )

        assertIs<LocalGameResult.Success<CreatedGame>>(result)
    }

    private fun loop(
        embeddingGenerator: ImageEmbeddingGenerator = TelemetryEmbeddingGenerator(),
        telemetry: OperationalTelemetry,
    ): LocalGameLoop = LocalGameLoop(
        identityRepository = TelemetryIdentityRepository(),
        gameRepository = TelemetryGameRepository(),
        progressRepository = TelemetryProgressRepository(),
        embeddingGenerator = embeddingGenerator,
        idGenerator = TelemetryIdGenerator(),
        telemetry = telemetry,
    )

    private fun image(): CapturedImage = CapturedImage.fromEncoded(byteArrayOf(1))
}

private class TelemetryIdentityRepository : PlayerIdentityRepository {
    override suspend fun current(): PlayerIdentity = PlayerIdentity(PlayerId("player-test"), "Agent")
}

private class TelemetryGameRepository : GameRepository {
    private val games = linkedMapOf<GameId, Game>()

    override suspend fun list(): List<Game> = games.values.toList()

    override suspend fun get(id: GameId): Game? = games[id]

    override suspend fun save(game: Game) {
        games[game.id] = game
    }
}

private class TelemetryProgressRepository : ThingProgressRepository {
    override suspend fun get(gameId: GameId, thingId: ThingId, playerId: PlayerId): ThingProgress? = null

    override suspend fun list(gameId: GameId, playerId: PlayerId): List<ThingProgress> = emptyList()

    override suspend fun save(progress: ThingProgress) = Unit
}

private class TelemetryEmbeddingGenerator(
    private val failure: Throwable? = null,
) : ImageEmbeddingGenerator {
    override suspend fun generate(image: CapturedImage): List<Float> {
        failure?.let { throw it }
        return unitEmbedding()
    }
}

private class FailAfterFirstEmbeddingGenerator : ImageEmbeddingGenerator {
    private var calls = 0

    override suspend fun generate(image: CapturedImage): List<Float> {
        calls += 1
        if (calls > 1) throw IllegalStateException("guess embedder unavailable")
        return unitEmbedding()
    }
}

private fun unitEmbedding(): List<Float> =
    List(IMAGE_EMBEDDING_DIMENSIONS) { index -> if (index == 0) 1f else 0f }

private class TelemetryIdGenerator : LocalGameIdGenerator {
    override fun nextGameId(): GameId = GameId("game-test")
    override fun nextThingId(): ThingId = ThingId("thing-test")
}
