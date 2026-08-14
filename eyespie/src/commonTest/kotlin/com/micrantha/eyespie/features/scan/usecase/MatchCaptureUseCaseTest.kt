package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.ImageEmbeddingContract
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.entities.toCanonicalEmbedding
import com.micrantha.eyespie.domain.repository.FakeThingRepository
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class MatchCaptureUseCaseTest {

    private class FakeImageEmbeddingGenerator : ImageEmbeddingGenerator {
        override suspend fun generate(image: CameraImage): Embedding =
            List(ImageEmbeddingContract.dimensions) { if (it == 0) 1f else 0f }
                .toCanonicalEmbedding()
    }

    private val generator = FakeImageEmbeddingGenerator()
    private val repository = FakeThingRepository()
    private val useCase = MatchCaptureUseCase(generator, repository)

    @Test
    fun `invoke should return match result`() = runTest {
        repository.matchResult = Result.success(emptyList())

        val result = useCase(image(), thing()).first().getOrThrow()

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
}
