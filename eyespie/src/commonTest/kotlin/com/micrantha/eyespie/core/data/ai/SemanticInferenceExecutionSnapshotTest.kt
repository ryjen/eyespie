package com.micrantha.eyespie.core.data.ai

import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.GenAIConfig
import com.micrantha.bluebell.platform.GenAIRequest
import com.micrantha.eyespie.domain.ai.InferenceLocality
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailability
import com.micrantha.eyespie.domain.ai.SemanticInferenceCapabilities
import com.micrantha.eyespie.domain.ai.SemanticInferenceExecutionConfiguration
import com.micrantha.eyespie.domain.ai.SemanticInferenceIdentity
import com.micrantha.eyespie.domain.ai.SemanticInferenceInitialization
import com.micrantha.eyespie.domain.ai.SemanticInferenceRequest
import com.micrantha.eyespie.domain.ai.SemanticInferenceSamplingConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

class SemanticInferenceExecutionSnapshotTest {

    @Test
    fun `generated output keeps the execution identity and configuration that produced it`() = runTest {
        val provider = GenAISemanticInferenceProvider(
            genAI = FakeGenAI(),
            identity = identity("bootstrap"),
            imageInputValidator = { true },
            initialAvailability = SemanticInferenceAvailability.NotConfigured,
        )
        val first = initialization("model-a", seed = 7)
        provider.initialize(first).getOrThrow()

        val output = provider.generateWithExecution(
            SemanticInferenceRequest(prompt = "structured clue")
        ).getOrThrow()

        val second = initialization("model-b", seed = 11)
        provider.initialize(second).getOrThrow()

        assertEquals(first.identity, output.execution.identity)
        assertEquals(
            SemanticInferenceExecutionConfiguration(
                sampling = first.sampling,
                maxImages = first.maxImages,
                maxContextTokens = first.capabilities.maxContextTokens,
            ),
            output.execution.configuration,
        )
        assertEquals(second.identity, provider.identity)
        assertEquals("ok", output.text)
    }

    private fun initialization(model: String, seed: Int) = SemanticInferenceInitialization(
        modelPath = "/models/$model.litertlm".toPath(),
        identity = identity(model),
        capabilities = SemanticInferenceCapabilities(
            textGeneration = true,
            imageInput = false,
            streaming = true,
            cancellation = true,
            maxContextTokens = 1024,
        ),
        maxImages = 1,
        sampling = SemanticInferenceSamplingConfiguration(
            topK = 40,
            topP = 0.95f,
            temperature = 0.8f,
            randomSeed = seed,
        ),
    )

    private fun identity(model: String) = SemanticInferenceIdentity(
        providerId = "mediapipe-local",
        runtimeId = "mediapipe-genai",
        locality = InferenceLocality.LOCAL,
        modelId = model,
        modelVersion = "1",
    )

    private class FakeGenAI : GenAI {
        override fun initialize(config: GenAIConfig) = Result.success(Unit)
        override fun newSession(config: GenAIConfig.Session) = Result.success(Unit)
        override fun generate(request: GenAIRequest) = Result.success("ok")
        override fun generateFlow(request: GenAIRequest): Flow<String> = flowOf("ok")
        override fun close() = Unit
        override fun cancel() = Unit
    }
}
