package com.micrantha.eyespie.core.data.ai

import com.micrantha.eyespie.core.data.ai.source.CluePromptSource
import com.micrantha.eyespie.domain.ai.InferenceLocality
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailability
import com.micrantha.eyespie.domain.ai.SemanticInferenceCapabilities
import com.micrantha.eyespie.domain.ai.SemanticInferenceExecutionSnapshot
import com.micrantha.eyespie.domain.ai.SemanticInferenceIdentity
import com.micrantha.eyespie.domain.ai.SemanticInferenceOutput
import com.micrantha.eyespie.domain.ai.SemanticInferenceProvider
import com.micrantha.eyespie.domain.ai.SemanticInferenceRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClueRepairExecutionTest {

    @Test
    fun `repair fails closed when provider execution changes between attempts`() = runTest {
        val firstIdentity = identity("model-a")
        val secondIdentity = identity("model-b")
        var currentIdentity = firstIdentity
        var calls = 0
        val provider = object : SemanticInferenceProvider {
            override val identity: SemanticInferenceIdentity
                get() = currentIdentity
            override val availability = MutableStateFlow<SemanticInferenceAvailability>(
                SemanticInferenceAvailability.Available(
                    SemanticInferenceCapabilities(
                        textGeneration = true,
                        imageInput = true,
                        streaming = false,
                    )
                )
            )

            override suspend fun generate(request: SemanticInferenceRequest): Result<String> =
                Result.failure(UnsupportedOperationException("generateWithExecution expected"))

            override suspend fun generateWithExecution(
                request: SemanticInferenceRequest,
            ): Result<SemanticInferenceOutput> {
                calls += 1
                return if (calls == 1) {
                    Result.success(
                        SemanticInferenceOutput(
                            text = "not json",
                            execution = SemanticInferenceExecutionSnapshot(firstIdentity, null),
                        )
                    )
                } else {
                    currentIdentity = secondIdentity
                    Result.success(
                        SemanticInferenceOutput(
                            text = """{"schemaVersion":1,"clues":[{"clue":"red thing","answer":"apple","confidence":0.9}]}""",
                            execution = SemanticInferenceExecutionSnapshot(secondIdentity, null),
                        )
                    )
                }
            }

            override fun generateFlow(request: SemanticInferenceRequest): Flow<String> = emptyFlow()
            override fun cancel() = Unit
            override suspend fun close() = Unit
        }
        val repository = ClueDataRepository(provider, CluePromptSource())

        val result = repository.clues("/test/image.jpg".toPath())

        assertTrue(result.exceptionOrNull() is GeneratedClueRepairExecutionChangedException)
        assertEquals(2, calls)
    }

    private fun identity(modelId: String) = SemanticInferenceIdentity(
        providerId = "test-local",
        runtimeId = "test-runtime",
        locality = InferenceLocality.LOCAL,
        modelId = modelId,
        modelVersion = "1",
    )
}
