package com.micrantha.eyespie.core.data.ai

import com.micrantha.eyespie.core.data.ai.source.CluePromptSource
import com.micrantha.eyespie.domain.ai.InferenceLocality
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailability
import com.micrantha.eyespie.domain.ai.SemanticInferenceCapabilities
import com.micrantha.eyespie.domain.ai.SemanticInferenceExecutionConfiguration
import com.micrantha.eyespie.domain.ai.SemanticInferenceIdentity
import com.micrantha.eyespie.domain.ai.SemanticInferenceProvider
import com.micrantha.eyespie.domain.ai.SemanticInferenceRequest
import com.micrantha.eyespie.domain.ai.SemanticInferenceSamplingConfiguration
import com.micrantha.eyespie.domain.entities.GuessClue
import com.micrantha.eyespie.domain.entities.toGuessClue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ClueDataRepositoryTest {

    private class FakeInferenceProvider : SemanticInferenceProvider {
        val requests = mutableListOf<SemanticInferenceRequest>()
        val responses = mutableListOf<Result<String>>()
        override var identity = SemanticInferenceIdentity(
            providerId = "test-local",
            runtimeId = "test-runtime",
            locality = InferenceLocality.LOCAL,
            modelId = "test-model",
            modelVersion = "v1",
        )
        override val executionConfiguration = SemanticInferenceExecutionConfiguration(
            sampling = SemanticInferenceSamplingConfiguration(
                topK = 40,
                topP = 0.95f,
                temperature = 0.8f,
                randomSeed = 7,
            ),
            maxImages = 1,
            maxContextTokens = 1024,
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
            return if (responses.isNotEmpty()) {
                responses.removeAt(0)
            } else {
                Result.failure(IllegalStateException("no fake response configured"))
            }
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
        override fun repair(candidate: String) = "mock repair prompt: $candidate"
    }

    private val provider = FakeInferenceProvider()
    private val cluePromptSource = FakeCluePromptSource()
    private val repository = ClueDataRepository(provider, cluePromptSource)

    @Test
    fun `clues should map valid one through three clue responses deterministically`() = runTest {
        for (count in 1..3) {
            provider.responses += Result.success(validResponse(count))

            val result = repository.clues("/test/image-$count.jpg".toPath()).getOrThrow()

            assertEquals(count, result.size)
            assertEquals((1..count).map { "clue $it" }.toSet(), result.map { it.data }.toSet())
        }
    }

    @Test
    fun `structured response should ignore unknown additive fields`() = runTest {
        provider.responses += Result.success(
            """{"schemaVersion":1,"future":"ignored","clues":[{"clue":"red thing","answer":"apple","confidence":0.9,"extra":true}]}"""
        )

        val clue = repository.clues("/test/image.jpg".toPath()).getOrThrow().single()

        assertEquals("red thing", clue.data)
        assertEquals("apple", clue.answer)
    }

    @Test
    fun `malformed local output should get one image-free repair attempt`() = runTest {
        provider.responses += Result.success("```json\n${validResponse(1)}\n```")
        provider.responses += Result.success(validResponse(1))
        val image = "/test/image.jpg".toPath()

        val envelope = repository.generateClueEnvelope(image).getOrThrow()

        assertTrue(envelope.provenance.repaired)
        assertEquals(2, provider.requests.size)
        assertEquals(listOf(image), provider.requests[0].images.map { it.localPath })
        assertTrue(provider.requests[1].images.isEmpty())
        assertTrue(provider.requests[1].prompt.startsWith("mock repair prompt:"))
    }

    @Test
    fun `repair should be bounded to one attempt`() = runTest {
        provider.responses += Result.success("not json")
        provider.responses += Result.success("still not json")

        val result = repository.generateClueEnvelope("/test/image.jpg".toPath())

        assertTrue(result.exceptionOrNull() is MalformedGeneratedClueResponseException)
        assertEquals(2, provider.requests.size)
    }

    @Test
    fun `remote provider should not resend malformed image-derived output for repair`() = runTest {
        provider.identity = provider.identity.copy(locality = InferenceLocality.REMOTE)
        provider.responses += Result.success("not json")

        val result = repository.generateClueEnvelope("/test/image.jpg".toPath())

        assertTrue(result.exceptionOrNull() is MalformedGeneratedClueResponseException)
        assertEquals(1, provider.requests.size)
    }

    @Test
    fun `repair provider failure should remain distinguishable from schema failure`() = runTest {
        val providerFailure = IllegalStateException("provider failed during repair")
        provider.responses += Result.success("not json")
        provider.responses += Result.failure(providerFailure)

        val result = repository.generateClueEnvelope("/test/image.jpg".toPath())

        assertSame(providerFailure, result.exceptionOrNull())
        assertFalse(result.exceptionOrNull() is GeneratedClueResponseException)
    }

    @Test
    fun `provider failure should remain distinguishable from response failure`() = runTest {
        val providerFailure = IllegalStateException("provider failed")
        provider.responses += Result.failure(providerFailure)

        val result = repository.clues("/test/image.jpg".toPath())

        assertSame(providerFailure, result.exceptionOrNull())
    }

    @Test
    fun `missing required field should return typed malformed response failure`() = runTest {
        disableRepair()
        provider.responses += Result.success(
            """{"schemaVersion":1,"clues":[{"clue":"red thing","confidence":0.9}]}"""
        )

        val result = repository.clues("/test/image.jpg".toPath())

        assertTrue(result.exceptionOrNull() is MalformedGeneratedClueResponseException)
    }

    @Test
    fun `unsupported schema should return typed schema failure`() = runTest {
        disableRepair()
        provider.responses += Result.success(
            """{"schemaVersion":2,"clues":[{"clue":"red thing","answer":"apple","confidence":0.9}]}"""
        )

        val result = repository.clues("/test/image.jpg".toPath())

        assertTrue(result.exceptionOrNull() is UnsupportedGeneratedClueSchemaException)
    }

    @Test
    fun `blank values out of range confidence and too many clues should return typed validation failures`() = runTest {
        val invalidResponses = listOf(
            """{"schemaVersion":1,"clues":[{"clue":" ","answer":"apple","confidence":0.9}]}""",
            """{"schemaVersion":1,"clues":[{"clue":"red thing","answer":" ","confidence":0.9}]}""",
            """{"schemaVersion":1,"clues":[{"clue":"red thing","answer":"apple","confidence":1.1}]}""",
            validResponse(4),
        )

        invalidResponses.forEachIndexed { index, response ->
            disableRepair()
            provider.responses += Result.success(response)

            val result = repository.clues("/test/invalid-$index.jpg".toPath())

            assertTrue(result.exceptionOrNull() is InvalidGeneratedClueResponseException)
        }
    }

    @Test
    fun `non finite confidence should be rejected as malformed JSON number`() = runTest {
        for (confidence in listOf("NaN", "Infinity", "-Infinity")) {
            disableRepair()
            provider.responses += Result.success(
                """{"schemaVersion":1,"clues":[{"clue":"red thing","answer":"apple","confidence":$confidence}]}"""
            )

            val result = repository.clues("/test/non-finite.jpg".toPath())

            assertTrue(result.exceptionOrNull() is MalformedGeneratedClueResponseException)
        }
    }

    @Test
    fun `prose and fenced JSON should be rejected without an allowed repair`() = runTest {
        val valid = validResponse(1)
        for (response in listOf("prefix $valid", "```json\n$valid\n```")) {
            disableRepair()
            provider.responses += Result.success(response)

            val result = repository.clues("/test/wrapped.jpg".toPath())

            assertTrue(result.exceptionOrNull() is MalformedGeneratedClueResponseException)
        }
    }

    @Test
    fun `provenance should come from application provider and prompt state`() = runTest {
        provider.responses += Result.success(
            """{"schemaVersion":1,"providerId":"model-lie","modelId":"model-lie","promptVersion":999,"clues":[{"clue":"red thing","answer":"apple","confidence":0.9}]}"""
        )

        val envelope = repository.generateClueEnvelope("/test/image.jpg".toPath()).getOrThrow()
        val provenance = envelope.provenance

        assertEquals(1, provenance.schemaVersion)
        assertEquals("test-local", provenance.providerId)
        assertEquals("test-runtime", provenance.runtimeId)
        assertEquals(InferenceLocality.LOCAL, provenance.locality)
        assertEquals("test-model", provenance.modelId)
        assertEquals("v1", provenance.modelVersion)
        assertEquals(cluePromptSource.cluePromptId, provenance.promptId)
        assertEquals(cluePromptSource.cluePromptVersion, provenance.promptVersion)
        assertEquals(provider.executionConfiguration, provenance.executionConfiguration)
        assertFalse(provenance.repaired)
    }

    @Test
    fun `guesser projection should contain clue text but not answer bearing fields`() {
        val generated = com.micrantha.eyespie.domain.entities.AiClue(
            data = "something red",
            confidence = 0.9f,
            answer = "apple",
        )

        val guesser = generated.toGuessClue()
        val encoded = Json.encodeToString(guesser)

        assertEquals(GuessClue("something red"), guesser)
        assertFalse(encoded.contains("apple"))
        assertFalse(encoded.contains("answer"))
        assertFalse(encoded.contains("confidence"))
    }

    @Test
    fun `clue prompt should request schema v1 raw JSON instead of line parsing`() {
        val prompt = CluePromptSource().clues()

        assertTrue(prompt.contains("raw JSON only"))
        assertTrue(prompt.contains("schemaVersion"))
        assertTrue(prompt.contains("exactly 1"))
        assertFalse(prompt.contains("three lines per clue"))
    }

    @Test
    fun `independent clue requests should contain only the current image`() = runTest {
        provider.responses += Result.success(validResponse(1))
        provider.responses += Result.success(validResponse(1))

        repository.clues("/test/a.jpg".toPath()).getOrThrow()
        repository.clues("/test/b.jpg".toPath()).getOrThrow()

        assertEquals(listOf("/test/a.jpg".toPath()), provider.requests[0].images.map { it.localPath })
        assertEquals(listOf("/test/b.jpg".toPath()), provider.requests[1].images.map { it.localPath })
    }

    @Test
    fun `guess should contain only current image and bounded prompt`() = runTest {
        provider.responses += Result.success("yes")
        val image = "/test/guess.jpg".toPath()

        val result = repository.guess(image, GuessClue("red thing"))

        assertTrue(result.isSuccess)
        assertEquals("mock guess prompt", provider.requests.single().prompt)
        assertEquals(listOf(image), provider.requests.single().images.map { it.localPath })
    }

    @Test
    fun `relative image path is rejected before provider inference`() = runTest {
        provider.responses += Result.success(validResponse(1))

        assertFailsWith<IllegalArgumentException> {
            repository.clues("relative/frame.jpg".toPath())
        }
        assertTrue(provider.requests.isEmpty())
    }

    private fun disableRepair() {
        provider.identity = provider.identity.copy(locality = InferenceLocality.REMOTE)
    }

    private fun validResponse(count: Int): String {
        val clues = (1..count).joinToString(",") { index ->
            """{"clue":"clue $index","answer":"answer $index","confidence":0.9}"""
        }
        return """{"schemaVersion":1,"clues":[$clues]}"""
    }
}
