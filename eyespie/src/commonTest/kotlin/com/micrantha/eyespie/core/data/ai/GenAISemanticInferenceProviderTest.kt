package com.micrantha.eyespie.core.data.ai

import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.GenAIConfig
import com.micrantha.bluebell.platform.GenAIRequest
import com.micrantha.eyespie.domain.ai.InferenceLocality
import com.micrantha.eyespie.domain.ai.InvalidSemanticInferenceRequestException
import com.micrantha.eyespie.domain.ai.SemanticImageInput
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailability
import com.micrantha.eyespie.domain.ai.SemanticInferenceCapabilities
import com.micrantha.eyespie.domain.ai.SemanticInferenceCapability
import com.micrantha.eyespie.domain.ai.SemanticInferenceIdentity
import com.micrantha.eyespie.domain.ai.SemanticInferenceRequest
import com.micrantha.eyespie.domain.ai.SemanticInferenceUnavailableException
import com.micrantha.eyespie.domain.ai.UnsupportedSemanticCapabilityException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GenAISemanticInferenceProviderTest {
    private val sessionConfig = GenAIConfig.Session(
        topK = 40,
        topP = 0.95f,
        temperature = 0.8f,
        randomSeed = 0,
        loraPath = "",
        enableVisionModality = true,
    )

    private val identity = SemanticInferenceIdentity(
        providerId = "mediapipe-local",
        runtimeId = "mediapipe-genai",
        locality = InferenceLocality.LOCAL,
        modelId = "fixture-model",
        modelVersion = "1",
    )

    @Test
    fun `unavailable provider fails before opening a runtime session`() = runTest {
        val genAI = FakeGenAI()
        val provider = provider(genAI, SemanticInferenceAvailability.NotConfigured)

        val result = provider.generate(SemanticInferenceRequest(prompt = "make a clue"))

        assertTrue(result.isFailure)
        assertIs<SemanticInferenceUnavailableException>(result.exceptionOrNull())
        assertEquals(0, genAI.newSessionCount)
        assertEquals(0, genAI.generateCount)
    }

    @Test
    fun `text only provider rejects raw image capability before delegation`() = runTest {
        val genAI = FakeGenAI()
        val provider = provider(genAI, available(imageInput = false))

        val result = provider.generate(
            SemanticInferenceRequest(
                prompt = "make a clue",
                images = listOf(SemanticImageInput("/tmp/frame.jpg".toPath())),
            )
        )

        val error = assertIs<UnsupportedSemanticCapabilityException>(result.exceptionOrNull())
        assertEquals(SemanticInferenceCapability.IMAGE_INPUT, error.capability)
        assertEquals(0, genAI.newSessionCount)
        assertEquals(0, genAI.generateCount)
    }

    @Test
    fun `relative image path fails closed before opening a runtime session`() = runTest {
        val genAI = FakeGenAI()
        val provider = provider(genAI, available(imageInput = true))

        val result = provider.generate(
            SemanticInferenceRequest(
                prompt = "make a clue",
                images = listOf(SemanticImageInput("relative/frame.jpg".toPath())),
            )
        )

        assertIs<InvalidSemanticInferenceRequestException>(result.exceptionOrNull())
        assertEquals(0, genAI.newSessionCount)
        assertEquals(0, genAI.generateCount)
    }

    @Test
    fun `streaming request rejects provider without streaming capability`() = runTest {
        val genAI = FakeGenAI()
        val provider = provider(genAI, available(streaming = false))

        val error = assertFailsWith<UnsupportedSemanticCapabilityException> {
            provider.generateFlow(SemanticInferenceRequest(prompt = "make a clue")).toList()
        }

        assertEquals(SemanticInferenceCapability.STREAMING, error.capability)
        assertEquals(0, genAI.newSessionCount)
    }

    @Test
    fun `separate requests use fresh closed runtime sessions`() = runTest {
        val genAI = FakeGenAI()
        val provider = provider(genAI, available())
        val request = SemanticInferenceRequest(prompt = "make a clue")

        assertEquals("ok", provider.generate(request).getOrThrow())
        assertEquals("ok", provider.generate(request).getOrThrow())

        assertEquals(2, genAI.newSessionCount)
        assertEquals(listOf(1, 2), genAI.generationSessions)
        assertEquals(2, genAI.closeCount)
        assertEquals(null, genAI.currentSession)
    }

    @Test
    fun `failed session creation is cleaned up and does not delegate generation`() = runTest {
        val genAI = FakeGenAI().apply {
            newSessionResult = Result.failure(IllegalStateException("session failed"))
        }
        val provider = provider(genAI, available())

        val result = provider.generate(SemanticInferenceRequest(prompt = "make a clue"))

        assertTrue(result.isFailure)
        assertEquals(1, genAI.newSessionCount)
        assertEquals(0, genAI.generateCount)
        assertEquals(1, genAI.closeCount)
        assertEquals(null, genAI.currentSession)
    }

    @Test
    fun `runtime cancellation returned as failure propagates while session cleanup still runs`() = runTest {
        val genAI = FakeGenAI().apply {
            generateResult = Result.failure(CancellationException("cancelled"))
        }
        val provider = provider(genAI, available())

        assertFailsWith<CancellationException> {
            provider.generate(SemanticInferenceRequest(prompt = "make a clue"))
        }

        assertEquals(1, genAI.closeCount)
        assertEquals(null, genAI.currentSession)
    }

    @Test
    fun `image input is encoded as a local file URI for the runtime adapter`() = runTest {
        val genAI = FakeGenAI()
        val provider = provider(genAI, available(imageInput = true))

        provider.generate(
            SemanticInferenceRequest(
                prompt = "make a clue",
                images = listOf(SemanticImageInput("/tmp/frame #1?.jpg".toPath())),
            )
        ).getOrThrow()

        assertEquals(
            listOf("file:///tmp/frame%20%231%3F.jpg"),
            genAI.requests.single().images,
        )
    }

    @Test
    fun `availability transitions are observable independently of provider identity`() {
        val provider = provider(FakeGenAI(), SemanticInferenceAvailability.NotConfigured)

        provider.markInitializing()
        assertIs<SemanticInferenceAvailability.Initializing>(provider.availability.value)

        provider.markAvailable(available(cancellation = true).capabilities)
        val available = assertIs<SemanticInferenceAvailability.Available>(provider.availability.value)
        assertTrue(available.capabilities.supports(SemanticInferenceCapability.CANCELLATION))

        provider.markFailed("model_init_failed")
        val failed = assertIs<SemanticInferenceAvailability.Failed>(provider.availability.value)
        assertEquals("model_init_failed", failed.diagnosticCode)

        assertEquals(identity, provider.identity)
    }

    private fun provider(
        genAI: FakeGenAI,
        availability: SemanticInferenceAvailability,
    ) = GenAISemanticInferenceProvider(
        genAI = genAI,
        identity = identity,
        sessionConfig = sessionConfig,
        initialAvailability = availability,
    )

    private fun available(
        imageInput: Boolean = false,
        streaming: Boolean = true,
        cancellation: Boolean = false,
    ) = SemanticInferenceAvailability.Available(
        SemanticInferenceCapabilities(
            textGeneration = true,
            imageInput = imageInput,
            streaming = streaming,
            cancellation = cancellation,
            maxContextTokens = 1024,
        )
    )

    private class FakeGenAI : GenAI {
        var newSessionResult: Result<Unit> = Result.success(Unit)
        var generateResult: Result<String> = Result.success("ok")
        var newSessionCount = 0
        var generateCount = 0
        var closeCount = 0
        var cancelCount = 0
        var currentSession: Int? = null
        val generationSessions = mutableListOf<Int>()
        val requests = mutableListOf<GenAIRequest>()

        override fun initialize(config: GenAIConfig) = Result.success(Unit)

        override fun newSession(config: GenAIConfig.Session): Result<Unit> {
            newSessionCount += 1
            currentSession = newSessionCount
            return newSessionResult
        }

        override fun generate(request: GenAIRequest): Result<String> {
            generateCount += 1
            requests += request
            currentSession?.let(generationSessions::add)
            return generateResult
        }

        override fun generateFlow(request: GenAIRequest): Flow<String> {
            requests += request
            currentSession?.let(generationSessions::add)
            return flowOf("one", "two")
        }

        override fun close() {
            closeCount += 1
            currentSession = null
        }

        override fun cancel() {
            cancelCount += 1
        }
    }
}
