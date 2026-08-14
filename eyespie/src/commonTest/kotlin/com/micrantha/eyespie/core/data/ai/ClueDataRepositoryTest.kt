package com.micrantha.eyespie.core.data.ai

import com.micrantha.eyespie.core.data.ai.source.CluePromptSource
import com.micrantha.eyespie.domain.ai.InferenceLocality
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailability
import com.micrantha.eyespie.domain.ai.SemanticInferenceCapabilities
import com.micrantha.eyespie.domain.ai.SemanticInferenceIdentity
import com.micrantha.eyespie.domain.ai.SemanticInferenceProvider
import com.micrantha.eyespie.domain.ai.SemanticInferenceRequest
import com.micrantha.eyespie.domain.entities.GuessClue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ClueDataRepositoryTest {

    private class FakeInferenceProvider : SemanticInferenceProvider {
        var generateResult: Result<String> = Result.failure(Exception("Not set"))
        val requests = mutableListOf<SemanticInferenceRequest>()
        override val identity = SemanticInferenceIdentity(
            providerId = "test-local",
            runtimeId = "test-runtime",
            locality = InferenceLocality.LOCAL,
        )
        override val availability = MutableStateFlow<SemanticInferenceAvailability>(
            SemanticInferenceAvailability.Available(
                SemanticInferenceCapabilities(
                    textGeneration = true,
                    imageInput = true,
                    streaming = true,
                    cancellation = true,
                )
            )
        )

        override suspend fun generate(request: SemanticInferenceRequest): Result<String> {
            requests += request
            return generateResult
        }

        override fun generateFlow(request: SemanticInferenceRequest): Flow<String> {
            requests += request
            return emptyFlow()
        }

        override suspend fun close() = Unit
        override fun cancel() = Unit
    }

    private class FakeCluePromptSource : CluePromptSource() {
        override fun clues() = "mock clues prompt"
        override fun guess(clue: String) = "mock guess prompt"
    }

    private val provider = FakeInferenceProvider()
    private val cluePromptSource = FakeCluePromptSource()
    private val repository = ClueDataRepository(provider, cluePromptSource)

    @Test
    fun `clues should return parsed proof when provider returns valid output`() = runTest {
        val imagePath = "/test/image.jpg".toPath()
        provider.generateResult = Result.success("clue\nanswer\n0.9")

        val result = repository.clues(imagePath)

        assertTrue(result.isSuccess)
        val clue = result.getOrThrow().single()
        assertEquals("clue", clue.data)
        assertEquals("answer", clue.answer)
        assertEquals(0.9f, clue.confidence)
        assertEquals("mock clues prompt", provider.requests.single().prompt)
        assertEquals(imagePath, provider.requests.single().images.single().localPath)
    }

    @Test
    fun `clues should return failure when provider fails`() = runTest {
        provider.generateResult = Result.failure(Exception("AI error"))

        val result = repository.clues("/test/image.jpg".toPath())

        assertTrue(result.isFailure)
    }

    @Test
    fun `independent clue requests should contain only the current image`() = runTest {
        provider.generateResult = Result.success("clue\nanswer\n0.9")

        repository.clues("/test/a.jpg".toPath()).getOrThrow()
        repository.clues("/test/b.jpg".toPath()).getOrThrow()

        assertEquals(listOf("/test/a.jpg".toPath()), provider.requests[0].images.map { it.localPath })
        assertEquals(listOf("/test/b.jpg".toPath()), provider.requests[1].images.map { it.localPath })
    }

    @Test
    fun `repeated clue request should still include the current image`() = runTest {
        provider.generateResult = Result.success("clue\nanswer\n0.9")
        val image = "/test/repeated.jpg".toPath()

        repository.clues(image).getOrThrow()
        repository.clues(image).getOrThrow()

        assertEquals(listOf(image), provider.requests[0].images.map { it.localPath })
        assertEquals(listOf(image), provider.requests[1].images.map { it.localPath })
    }

    @Test
    fun `guess should contain only current image and bounded prompt`() = runTest {
        provider.generateResult = Result.success("yes")
        val image = "/test/guess.jpg".toPath()

        val result = repository.guess(image, GuessClue("red thing"))

        assertTrue(result.isSuccess)
        assertEquals("mock guess prompt", provider.requests.single().prompt)
        assertEquals(listOf(image), provider.requests.single().images.map { it.localPath })
    }

    @Test
    fun `relative image path is rejected before provider inference`() = runTest {
        provider.generateResult = Result.success("clue\nanswer\n0.9")

        assertFailsWith<IllegalArgumentException> {
            repository.clues("relative/frame.jpg".toPath())
        }
        assertTrue(provider.requests.isEmpty())
    }
}
