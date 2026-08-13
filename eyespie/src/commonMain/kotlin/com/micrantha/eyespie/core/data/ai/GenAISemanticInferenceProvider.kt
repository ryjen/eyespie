package com.micrantha.eyespie.core.data.ai

import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.GenAIRequest
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailability
import com.micrantha.eyespie.domain.ai.SemanticInferenceIdentity
import com.micrantha.eyespie.domain.ai.SemanticInferenceProvider
import com.micrantha.eyespie.domain.ai.SemanticInferenceRequest
import kotlinx.coroutines.flow.Flow

internal class GenAISemanticInferenceProvider(
    private val genAI: GenAI,
    override val identity: SemanticInferenceIdentity,
    override val availability: SemanticInferenceAvailability,
) : SemanticInferenceProvider {
    override fun generate(request: SemanticInferenceRequest) =
        genAI.generate(GenAIRequest(request.prompt, request.images))

    override fun generateFlow(request: SemanticInferenceRequest): Flow<String> =
        genAI.generateFlow(GenAIRequest(request.prompt, request.images))

    override fun cancel() = genAI.cancel()
    override fun close() = genAI.close()
}
