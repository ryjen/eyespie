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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GenAISemanticInferenceProviderTest {
    private val identity = SemanticInferenceIdentity(
        providerId = "mediapipe-local",
        runtimeId = "mediapipe-genai",
        locality = InferenceLocality.LOCAL,
        modelId = "fixture-model",
        modelVersion = "1",
    )

    @Test
    fun `unavailable provider fails before runtime delegation`() = runTest {
        val genAI = FakeGenAI()
        val provider = provider(genAI, SemanticInferenceAvailability.NotConfigured)

        val result = provider.generate(SemanticInferenceRequest(prompt = "make a clue"))

        assertIs<SemanticInferenceUnavailableException>(result.exceptionOrNull())
        assertEquals(0, genAI.generateCount)
        assertEquals(0, genAI.newSessionCount)
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
        assertEquals(0, genAI.generateCount)
    }

    @Test
    fun `relative image path fails closed before runtime delegation`() = runTest {
        val genAI = FakeGenAI()
        val provider = provider(genAI, available(imageInput = true))

        val result = provider.generate(
            SemanticInferenceRequest(
                prompt = "make a clue",
                images = listOf(SemanticImageInput("relative/frame.jpg".toPath())),
            )
        )

        assertIs<InvalidSemanticInferenceRequestException>(result.exceptionOrNull())
        assertEquals(0, genAI.generateCount)
    }

    @Test
    fun `unloadable image fails closed before runtime delegation`() = runTest {
        val genAI = FakeGenAI()
        val provider = provider(
            genAI = genAI,
            availability = available(imageInput = true),
            imageInputValidator = { false },
        )

        val result = provider.generate(
            SemanticInferenceRequest(
                prompt = "make a clue",
                images = listOf(SemanticImageInput("/tmp/missing.jpg".toPath())),
            )
        )

        assertIs<InvalidSemanticInferenceRequestException>(result.exceptionOrNull())
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
        assertTrue(genAI.requests.isEmpty())
    }

    @Test
    fun `separate logical requests delegate without provider owned sessions`() = runTest {
        val genAI = FakeGenAI()
        val provider = provider(genAI, available())
        val request = SemanticInferenceRequest(prompt = "make a clue")

        assertEquals("ok", provider.generate(request).getOrThrow())
        assertEquals("ok", provider.generate(request).getOrThrow())

        assertEquals(2, genAI.generateCount)
        assertEquals(0, genAI.newSessionCount)
        assertEquals(0, genAI.closeCount)
    }

    @Test
    fun `runtime cancellation returned as failure propagates without provider teardown`() = runTest {
        val genAI = FakeGenAI().apply {
            generateResult = Result.failure(CancellationException("cancelled"))
        }
        val provider = provider(genAI, available())

        assertFailsWith<CancellationException> {
            provider.generate(SemanticInferenceRequest(prompt = "make a clue"))
        }

        assertEquals(0, genAI.closeCount)
    }

    @Test
    fun `close cancels active stream then closes runtime and blocks later generation`() = runTest {
        val genAI = FakeGenAI().apply { holdStreamUntilCancelled = true }
        val provider = provider(genAI, available())
        val collecting = launch {
            provider.generateFlow(SemanticInferenceRequest(prompt = "make a clue")).toList()
        }
        genAI.streamStarted.await()

        provider.close()
        collecting.join()

        val unavailable = assertIs<SemanticInferenceAvailability.Unavailable>(provider.availability.value)
        assertEquals("provider_closed", unavailable.reasonCode)
        assertEquals(1, genAI.cancelCount)
        assertEquals(1, genAI.closeCount)
        val result = provider.generate(SemanticInferenceRequest(prompt = "another clue"))
        assertIs<SemanticInferenceUnavailableException>(result.exceptionOrNull())
        assertEquals(1, genAI.requests.size)
    }

    @Test
    fun `cold stream cannot start after provider closes`() = runTest {
        val genAI = FakeGenAI()
        val provider = provider(genAI, available())
        val pending = provider.generateFlow(SemanticInferenceRequest(prompt = "make a clue"))

        provider.close()

        assertFailsWith<SemanticInferenceUnavailableException> { pending.toList() }
        assertTrue(genAI.requests.isEmpty())
    }

    @Test
    fun `image input is encoded as local file URI at runtime adapter boundary`() = runTest {
        val genAI = FakeGenAI()
        val provider = provider(genAI, available(imageInput = true))

        provider.generate(
            SemanticInferenceRequest(
                prompt = "make a clue",
                images = listOf(SemanticImageInput("/tmp/frame #1?.jpg".toPath())),
            )
        ).getOrThrow()

        assertEquals(listOf("file:///tmp/frame%20%231%3F.jpg"), genAI.requests.single().images)
    }

    @Test
    fun `availability transitions remain application owned`() = runTest {
        val provider = provider(FakeGenAI(), SemanticInferenceAvailability.NotConfigured)

        provider.markInitializing()
        assertIs<SemanticInferenceAvailability.Initializing>(provider.availability.value)
        provider.markAvailable(available(cancellation = true).capabilities)
        val available = assertIs<SemanticInferenceAvailability.Available>(provider.availability.value)
        assertTrue(available.capabilities.supports(SemanticInferenceCapability.CANCELLATION))
        provider.markFailed("model_init_failed")
        assertEquals(
            "model_init_failed",
            assertIs<SemanticInferenceAvailability.Failed>(provider.availability.value).diagnosticCode,
        )
        assertEquals(identity, provider.identity)
    }

    private fun provider(
        genAI: FakeGenAI,
        availability: SemanticInferenceAvailability,
        imageInputValidator: (Path) -> Boolean = { true },
    ) = GenAISemanticInferenceProvider(
        genAI = genAI,
        identity = identity,
        imageInputValidator = imageInputValidator,
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
        var generateResult: Result<String> = Result.success("ok")
        var holdStreamUntilCancelled = false
        val streamStarted = CompletableDeferred<Unit>()
        private val streamCancelled = CompletableDeferred<Unit>()
        var newSessionCount = 0
        var generateCount = 0
        var closeCount = 0
        var cancelCount = 0
        val requests = mutableListOf<GenAIRequest>()

        override fun initialize(config: GenAIConfig) = Result.success(Unit)

        override fun newSession(config: GenAIConfig.Session): Result<Unit> {
            newSessionCount += 1
            return Result.success(Unit)
        }

        override fun generate(request: GenAIRequest): Result<String> {
            generateCount += 1
            requests += request
            return generateResult
        }

        override fun generateFlow(request: GenAIRequest): Flow<String> {
            requests += request
            if (holdStreamUntilCancelled) return flow {
                streamStarted.complete(Unit)
                emit("one")
                streamCancelled.await()
            }
            return flowOf("one", "two")
        }

        override fun close() {
            closeCount += 1
        }

        override fun cancel() {
            cancelCount += 1
            streamCancelled.complete(Unit)
        }
    }
}
