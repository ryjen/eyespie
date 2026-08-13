package com.micrantha.eyespie.core.data.ai

import com.micrantha.bluebell.platform.GenAI
import com.micrantha.bluebell.platform.GenAIConfig
import com.micrantha.bluebell.platform.GenAIRequest
import com.micrantha.eyespie.domain.ai.InvalidSemanticInferenceRequestException
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailability
import com.micrantha.eyespie.domain.ai.SemanticInferenceAvailabilityController
import com.micrantha.eyespie.domain.ai.SemanticInferenceCapabilities
import com.micrantha.eyespie.domain.ai.SemanticInferenceCapability
import com.micrantha.eyespie.domain.ai.SemanticInferenceIdentity
import com.micrantha.eyespie.domain.ai.SemanticInferenceProvider
import com.micrantha.eyespie.domain.ai.SemanticInferenceRequest
import com.micrantha.eyespie.domain.ai.SemanticInferenceUnavailableException
import com.micrantha.eyespie.domain.ai.UnsupportedSemanticCapabilityException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class GenAISemanticInferenceProvider(
    private val genAI: GenAI,
    override val identity: SemanticInferenceIdentity,
    private val sessionConfig: GenAIConfig.Session,
    initialAvailability: SemanticInferenceAvailability = SemanticInferenceAvailability.NotConfigured,
) : SemanticInferenceProvider, SemanticInferenceAvailabilityController {
    private val state = MutableStateFlow(initialAvailability)
    private val generationMutex = Mutex()

    override val availability = state.asStateFlow()

    override suspend fun generate(request: SemanticInferenceRequest): Result<String> =
        generationMutex.withLock {
            try {
                validate(request, streaming = false)
                try {
                    genAI.newSession(sessionConfig).getOrThrow()
                    genAI.generate(request.toGenAIRequest())
                } finally {
                    genAI.close()
                }
            } catch (error: Throwable) {
                Result.failure(error)
            }
        }

    override fun generateFlow(request: SemanticInferenceRequest): Flow<String> = flow {
        generationMutex.withLock {
            validate(request, streaming = true)
            try {
                genAI.newSession(sessionConfig).getOrThrow()
                emitAll(genAI.generateFlow(request.toGenAIRequest()))
            } finally {
                genAI.close()
            }
        }
    }

    override fun cancel() = genAI.cancel()

    override fun close() {
        genAI.cancel()
        genAI.close()
    }

    override fun markNotConfigured() {
        state.value = SemanticInferenceAvailability.NotConfigured
    }

    override fun markInitializing() {
        state.value = SemanticInferenceAvailability.Initializing
    }

    override fun markAvailable(capabilities: SemanticInferenceCapabilities) {
        state.value = SemanticInferenceAvailability.Available(capabilities)
    }

    override fun markUnavailable(reasonCode: String) {
        state.value = SemanticInferenceAvailability.Unavailable(reasonCode)
    }

    override fun markFailed(diagnosticCode: String) {
        state.value = SemanticInferenceAvailability.Failed(diagnosticCode)
    }

    private fun validate(request: SemanticInferenceRequest, streaming: Boolean) {
        val current = state.value
        val capabilities = (current as? SemanticInferenceAvailability.Available)?.capabilities
            ?: throw SemanticInferenceUnavailableException(current)

        if (request.prompt.isBlank()) {
            throw InvalidSemanticInferenceRequestException()
        }
        requireCapability(capabilities, SemanticInferenceCapability.TEXT_GENERATION)
        if (request.images.isNotEmpty()) {
            requireCapability(capabilities, SemanticInferenceCapability.IMAGE_INPUT)
        }
        if (streaming) {
            requireCapability(capabilities, SemanticInferenceCapability.STREAMING)
        }
    }

    private fun requireCapability(
        capabilities: SemanticInferenceCapabilities,
        capability: SemanticInferenceCapability,
    ) {
        if (!capabilities.supports(capability)) {
            throw UnsupportedSemanticCapabilityException(capability)
        }
    }

    private fun SemanticInferenceRequest.toGenAIRequest() = GenAIRequest(
        prompt = prompt,
        images = images.map { "file://${it.localPath}" },
    )
}
