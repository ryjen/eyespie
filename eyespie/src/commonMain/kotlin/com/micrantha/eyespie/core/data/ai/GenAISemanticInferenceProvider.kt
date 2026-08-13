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
    private val imageInputValidator: (Path) -> Boolean,
    initialAvailability: SemanticInferenceAvailability = SemanticInferenceAvailability.NotConfigured,
) : SemanticInferenceProvider, SemanticInferenceAvailabilityController {
    private val state = MutableStateFlow(initialAvailability)
    private val generationMutex = Mutex()
    override val availability = state.asStateFlow()

    override suspend fun generate(request: SemanticInferenceRequest): Result<String> = generationMutex.withLock {
        validate(request, false).exceptionOrNull()?.let { return@withLock Result.failure(it) }
        var result: Result<String> = Result.failure(IllegalStateException("generation not started"))
        var cleanupFailure: Throwable? = null
        try {
            unavailableFailure()?.let { result = Result.failure(it) } ?: run {
                val sessionFailure = genAI.newSession(sessionConfig).failureOrCancellation()
                if (sessionFailure != null) {
                    result = Result.failure(sessionFailure)
                } else {
                    unavailableFailure()?.let { result = Result.failure(it) } ?: run {
                        result = genAI.generate(request.toGenAIRequest()).also { it.failureOrCancellation() }
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            result = Result.failure(error)
        } finally {
            try { genAI.close() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Throwable) { cleanupFailure = error }
        }
        cleanupFailure?.let { if (result.isSuccess) return@withLock Result.failure(it) }
        result
    }

    override fun generateFlow(request: SemanticInferenceRequest): Flow<String> = flow {
        generationMutex.withLock {
            validate(request, true).getOrThrow()
            var primaryFailure: Throwable? = null
            try {
                unavailableFailure()?.let { throw it }
                genAI.newSession(sessionConfig).getOrThrow()
                unavailableFailure()?.let { throw it }
                emitAll(genAI.generateFlow(request.toGenAIRequest()))
            } catch (error: Throwable) {
                primaryFailure = error
                throw error
            } finally {
                try {
                    genAI.close()
                } catch (cleanup: Throwable) {
                    if (primaryFailure == null) throw cleanup
                }
            }
        }
    }

    override fun cancel() = genAI.cancel()

    override suspend fun close() {
        state.value = SemanticInferenceAvailability.Unavailable(PROVIDER_CLOSED)
        var cancelFailure: Throwable? = null
        try {
            genAI.cancel()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            cancelFailure = error
        }

        var closeFailure: Throwable? = null
        generationMutex.withLock {
            try {
                genAI.close()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                closeFailure = error
            }
        }
        cancelFailure?.let { throw it }
        closeFailure?.let { throw it }
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
        val capabilities = (state.value as? SemanticInferenceAvailability.Available)?.capabilities
            ?: return Result.failure(SemanticInferenceUnavailableException(state.value))
        if (request.prompt.isBlank()) return Result.failure(InvalidSemanticInferenceRequestException())
        unsupported(capabilities, SemanticInferenceCapability.TEXT_GENERATION)?.let { return Result.failure(it) }
        if (request.images.isNotEmpty()) {
            unsupported(capabilities, SemanticInferenceCapability.IMAGE_INPUT)?.let { return Result.failure(it) }
            if (request.images.any { !it.localPath.isAbsolute || !validImage(it.localPath) }) {
                return Result.failure(InvalidSemanticInferenceRequestException())
            }
        }
        if (streaming) unsupported(capabilities, SemanticInferenceCapability.STREAMING)?.let { return Result.failure(it) }
        return Result.success(Unit)
    }

    private fun unavailableFailure(): SemanticInferenceUnavailableException? {
        val current = state.value
        return if (current is SemanticInferenceAvailability.Available) null
        else SemanticInferenceUnavailableException(current)
    }

    private fun validImage(path: Path): Boolean = try { imageInputValidator(path) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Throwable) { false }

    private fun unsupported(capabilities: SemanticInferenceCapabilities, capability: SemanticInferenceCapability) =
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
                    append('%'); append(digits[value ushr 4]); append(digits[value and 15])
                }
            }
        }
    }
    private companion object { const val PROVIDER_CLOSED = "provider_closed" }
}
