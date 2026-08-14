package com.micrantha.eyespie.core.data.ai

import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.GenAIConfig
import com.micrantha.bluebell.platform.GenAIRequest
import com.micrantha.eyespie.core.data.ai.source.CluePromptSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ClueDataRepositoryTest {

    private class FakeGenAI : GenAI {
        var generateResult: Result<String> = Result.failure(Exception("Not set"))
        val requests = mutableListOf<GenAIRequest>()

        override fun initialize(config: GenAIConfig) = Result.success(Unit)
        override fun newSession(config: GenAIConfig.Session) = Result.success(Unit)
        override fun generate(request: GenAIRequest): Result<String> {
            requests += request
            return generateResult
        }

        override fun generateFlow(request: GenAIRequest): Flow<String> = emptyFlow()
        override fun close() = Unit
        override fun cancel() = Unit
    }

    private class FakeCluePromptSource : CluePromptSource() {
        override fun clues() = "mock clues prompt"
        override fun guess(clue: String) = "mock guess prompt"
    }

    private val llm = FakeGenAI()
    private val cluePromptSource = FakeCluePromptSource()
    private val repository = ClueDataRepository(llm, cluePromptSource)

    @Test
    fun `clues should return parsed proof when llm returns valid output`() = runTest {
        val imagePath = "/test/image.jpg".toPath()
        val mockOutput = "clue\nanswer\n0.9"
        llm.generateResult = Result.success(mockOutput)

        val result = repository.clues(imagePath)

        assertTrue(result.isSuccess)
        val proof = result.getOrThrow()
        assertEquals(1, proof.size)
        val clue = proof.first()
        assertEquals("clue", clue.data)
        assertEquals("answer", clue.answer)
        assertEquals(0.9f, clue.confidence)
        assertEquals(listOf("file:///test/image.jpg"), llm.requests.single().images)
    }

    @Test
    fun `clues should return failure when llm fails`() = runTest {
        val imagePath = "/test/image.jpg".toPath()
        llm.generateResult = Result.failure(Exception("AI error"))

        val result = repository.clues(imagePath)

        assertTrue(result.isFailure)
    }

    @Test
    fun `independent clue requests should contain only the current image`() = runTest {
        llm.generateResult = Result.success("clue\nanswer\n0.9")

        repository.clues("/test/a.jpg".toPath()).getOrThrow()
        repository.clues("/test/b.jpg".toPath()).getOrThrow()

        assertEquals(
            listOf("file:///test/a.jpg"),
            llm.requests[0].images
        )
        assertEquals(
            listOf("file:///test/b.jpg"),
            llm.requests[1].images
        )
    }

    @Test
    fun `repeated clue request should still include the current image`() = runTest {
        llm.generateResult = Result.success("clue\nanswer\n0.9")
        val image = "/test/repeated.jpg".toPath()

        repository.clues(image).getOrThrow()
        repository.clues(image).getOrThrow()

        assertEquals(
            listOf("file:///test/repeated.jpg"),
            llm.requests[0].images
        )
        assertEquals(
            listOf("file:///test/repeated.jpg"),
            llm.requests[1].images
        )
    }

    @Test
    fun `guess should contain only current image and bounded prompt`() = runTest {
        llm.generateResult = Result.success("yes")

        val result = repository.guess(
            "/test/guess.jpg".toPath(),
            com.micrantha.eyespie.domain.entities.GuessClue("red thing")
        )

        assertTrue(result.isSuccess)
        assertEquals("mock guess prompt", llm.requests.single().prompt)
        assertEquals(listOf("file:///test/guess.jpg"), llm.requests.single().images)
    }

    @Test
    fun `image path reserved characters are percent encoded`() = runTest {
        llm.generateResult = Result.success("clue\nanswer\n0.9")

        repository.clues("/test/frame #1?.jpg".toPath()).getOrThrow()

        assertEquals(
            listOf("file:///test/frame%20%231%3F.jpg"),
            llm.requests.single().images
        )
    }

    @Test
    fun `relative image path is rejected before inference`() = runTest {
        llm.generateResult = Result.success("clue\nanswer\n0.9")

        assertFailsWith<IllegalArgumentException> {
            repository.clues("relative/frame.jpg".toPath())
        }
        assertTrue(llm.requests.isEmpty())
    }
}
