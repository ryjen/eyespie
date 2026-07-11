package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.repository.FakeThingRepository
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.toByteString
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class MatchCaptureUseCaseTest {

    private class FakeImageEmbeddingGenerator : ImageEmbeddingGenerator {
        override suspend fun generate(image: CameraImage): Embedding = byteArrayOf(1, 2, 3, 4).toByteString()
    }

    private val generator = FakeImageEmbeddingGenerator()
    private val repository = FakeThingRepository()
    private val useCase = MatchCaptureUseCase(generator, repository)

    @Test
    fun `invoke should return match result`() = runTest {
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

        repository.matchResult = Result.success(emptyList())

        val result = useCase(image, thing).first().getOrThrow()

        assertFalse(result.matched)
    }
}
