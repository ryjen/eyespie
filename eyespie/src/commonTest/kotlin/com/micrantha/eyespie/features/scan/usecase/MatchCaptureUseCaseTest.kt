package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.ImageEmbeddingContract
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.entities.toCanonicalEmbedding
import com.micrantha.eyespie.domain.repository.FakeThingRepository
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class MatchCaptureUseCaseTest {

    private class FakeImageEmbeddingGenerator : ImageEmbeddingGenerator {
        override suspend fun generate(image: CameraImage): Embedding = embedding()
    }

    private val generator = FakeImageEmbeddingGenerator()
    private val repository = FakeThingRepository()
    private val useCase = MatchCaptureUseCase(generator, repository)

    @Test
    fun `invoke should return target-specific match result`() = runTest {
        repository.matchResult = Result.success(
            Thing.Match(id = "1", similarity = 0.82f, matched = true)
        )

        val result = useCase(image(), thing()).first().getOrThrow()

        assertTrue(result.matched)
        assertEquals(0.82f, result.bestSimilarity)
        assertEquals("1", repository.matchedThingID)
    }

    @Test
    fun `invoke should remain suspended until embedding generation completes`() = runTest {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val blockingGenerator = object : ImageEmbeddingGenerator {
            override suspend fun generate(image: CameraImage): Embedding {
                started.complete(Unit)
                release.await()
                return embedding()
            }
        }
        val blockingUseCase = MatchCaptureUseCase(blockingGenerator, repository)

        val invocation = async { blockingUseCase(image(), thing()) }
        started.await()

        assertFalse(invocation.isCompleted)

        release.complete(Unit)
        val result = invocation.await().first().getOrThrow()

        assertFalse(result.matched)
    }

    @Test
    fun `embedding failure should be returned through result flow`() = runTest {
        val expected = IllegalStateException("embedding failed")
        val failingGenerator = object : ImageEmbeddingGenerator {
            override suspend fun generate(image: CameraImage): Embedding = throw expected
        }
        val failingUseCase = MatchCaptureUseCase(failingGenerator, repository)

        val result = failingUseCase(image(), thing()).first()

        assertTrue(result.isFailure)
        assertSame(expected, result.exceptionOrNull())
    }

    @Test
    fun `embedding cancellation should propagate`() = runTest {
        val failingGenerator = object : ImageEmbeddingGenerator {
            override suspend fun generate(image: CameraImage): Embedding =
                throw CancellationException("cancelled")
        }
        val failingUseCase = MatchCaptureUseCase(failingGenerator, repository)

        assertFailsWith<CancellationException> {
            failingUseCase(image(), thing())
        }
    }

    @Test
    fun `repository flow exception should propagate`() = runTest {
        val expected = IllegalStateException("repository failed")
        repository.matchFlow = flow { throw expected }

        val resultFlow = useCase(image(), thing())
        val actual = assertFailsWith<IllegalStateException> {
            resultFlow.first()
        }

        assertSame(expected, actual)
    }

    private fun image() = object : CameraImage {
        override val width = 0
        override val height = 0
        override fun toByteArray() = byteArrayOf()
        override fun toImageBitmap() = TODO()
    }

    private fun thing() = Thing(
        id = "1",
        createdAt = Instant.parse("2023-01-01T00:00:00Z"),
        createdBy = Player.Ref("p1", "player"),
        guessed = false,
        guesses = emptyList(),
        imageUrl = "url",
        location = com.micrantha.eyespie.domain.entities.Location.Point(0.0, 0.0)
    )

    companion object {
        private fun embedding(): Embedding =
            List(ImageEmbeddingContract.dimensions) { if (it == 0) 1f else 0f }
                .toCanonicalEmbedding()
    }
}
