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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Path

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
            validate(request, streaming = false).exceptionOrNull()?.let {
                return@withLock Result.failure(it)
            }
            try {
                genAI.newSession(sessionConfig).failureOrCancellation()?.let {
                    return@withLock Result.failure(it)
                }
                genAI.generate(request.toGenAIRequest()).also { result ->
                    result.failureOrCancellation()
                }
            } finally {
                genAI.close()
            }
        }

    override fun generateFlow(request: SemanticInferenceRequest): Flow<String> = flow {
        generationMutex.withLock {
            validate(request, streaming = true).getOrThrow()
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

    override fun markNotConfigured() { state.value = SemanticInferenceAvailability.NotConfigured }
    override fun markInitializing() { state.value = SemanticInferenceAvailability.Initializing }
    override fun markAvailable(capabilities: SemanticInferenceCapabilities) {
        state.value = SemanticInferenceAvailability.Available(capabilities)
    }
    override fun markUnavailable(reasonCode: String) {
        state.value = SemanticInferenceAvailability.Unavailable(reasonCode)
    }
    override fun markFailed(diagnosticCode: String) {
        state.value = SemanticInferenceAvailability.Failed(diagnosticCode)
    }

    private fun validate(request: SemanticInferenceRequest, streaming: Boolean): Result<Unit> {
        val current = state.value
        val capabilities = (current as? SemanticInferenceAvailability.Available)?.capabilities
            ?: return Result.failure(SemanticInferenceUnavailableException(current))
        if (request.prompt.isBlank()) return Result.failure(InvalidSemanticInferenceRequestException())
        unsupported(capabilities, SemanticInferenceCapability.TEXT_GENERATION)?.let { return Result.failure(it) }
        if (request.images.isNotEmpty()) {
            unsupported(capabilities, SemanticInferenceCapability.IMAGE_INPUT)?.let { return Result.failure(it) }
            if (request.images.any { !it.localPath.isAbsolute }) {
                return Result.failure(InvalidSemanticInferenceRequestException())
            }
        }
        if (streaming) {
            unsupported(capabilities, SemanticInferenceCapability.STREAMING)?.let { return Result.failure(it) }
        }
        return Result.success(Unit)
    }

    private fun unsupported(
        capabilities: SemanticInferenceCapabilities,
        capability: SemanticInferenceCapability,
    ): UnsupportedSemanticCapabilityException? =
        if (capabilities.supports(capability)) null else UnsupportedSemanticCapabilityException(capability)

    private fun <T> Result<T>.failureOrCancellation(): Throwable? = exceptionOrNull()?.also {
        if (it is CancellationException) throw it
    }

    private fun SemanticInferenceRequest.toGenAIRequest() = GenAIRequest(
        prompt = prompt,
        images = images.map { it.localPath.asFileUri() },
    )

    private fun Path.asFileUri(): String = "file://" + toString().encodePathForUri()

    private fun String.encodePathForUri(): String {
        val digits = "0123456789ABCDEF"
        return buildString {
            encodeToByteArray().forEach { byte ->
                val value = byte.toInt() and 255
                val safe = value in 48..57 || value in 65..90 || value in 97..122 ||
                    value == 45 || value == 46 || value == 95 || value == 126 || value == 47
                if (safe) append(value.toChar()) else {
                    append('%')
                    append(digits[value ushr 4])
                    append(digits[value and 15])
                }
            }
        }
    }
}
