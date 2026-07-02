package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.toByteString
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class MatchCaptureUseCaseTest {

    private class FakeImageEmbeddingGenerator : ImageEmbeddingGenerator {
        override suspend fun generate(image: CameraImage): Embedding = byteArrayOf(1).toByteString()
    }

    private val generator = FakeImageEmbeddingGenerator()
    private val useCase = MatchCaptureUseCase(generator)

    @Test
    fun `invoke should throw UnsupportedOperationException`() = runTest {
        val image = object : CameraImage {
            override val width = 0
            override val height = 0
            override fun toByteArray() = byteArrayOf()
            override fun toImageBitmap() = TODO()
        }
        val thing = Thing(
            id = "1",
            createdAt = Instant.parse("2023-01-01T00:00:00Z"),
            createdBy = Player.Ref("p1", "player"),
            guessed = false,
            guesses = emptyList(),
            imageUrl = "url",
            location = com.micrantha.eyespie.domain.entities.Location.Point(0.0, 0.0)
        )

        assertFailsWith<UnsupportedOperationException> {
            useCase(image, thing).getOrThrow()
        }
    }
}
