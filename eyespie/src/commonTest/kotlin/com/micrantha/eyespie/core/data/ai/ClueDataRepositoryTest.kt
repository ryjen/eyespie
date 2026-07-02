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
import kotlin.test.assertTrue

class ClueDataRepositoryTest {

    private class FakeGenAI : GenAI {
        var generateResult: Result<String> = Result.failure(Exception("Not set"))

        override fun initialize(config: GenAIConfig) = Result.success(Unit)
        override fun newSession(config: GenAIConfig.Session) = Result.success(Unit)
        override fun generate(request: GenAIRequest) = generateResult
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
    }

    @Test
    fun `clues should return failure when llm fails`() = runTest {
        val imagePath = "/test/image.jpg".toPath()
        llm.generateResult = Result.failure(Exception("AI error"))

        val result = repository.clues(imagePath)

        assertTrue(result.isFailure)
    }
}
