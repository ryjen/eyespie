package com.micrantha.eyespie.core.data.ai

import com.micrantha.bluebell.platform.GenAI
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Path

/**
 * Eyespie-owned adapter from the runtime-neutral semantic inference contract to Bluebell GenAI.
 *
 * Request-session construction deliberately remains owned by the concrete GenAI runtime. Android
 * PlatformGenAI validates its session configuration once during initialization and creates a fresh
 * native MediaPipe session for each logical request. Repeating that lifecycle here would create a
 * second owner and could re-introduce teardown/cancellation races.
 */
internal class GenAISemanticInferenceProvider(
    private val genAI: GenAI,
    override val identity: SemanticInferenceIdentity,
    private val imageInputValidator: (Path) -> Boolean,
    initialAvailability: SemanticInferenceAvailability = SemanticInferenceAvailability.NotConfigured,
) : SemanticInferenceProvider, SemanticInferenceAvailabilityController {
    private val state = MutableStateFlow(initialAvailability)
    private val generationMutex = Mutex()
    private val lifecycleMutex = Mutex()
    private val closeMutex = Mutex()
    private var closed = false

    override val availability = state.asStateFlow()

    override suspend fun generate(request: SemanticInferenceRequest): Result<String> =
        generationMutex.withLock {
            validate(request, streaming = false).exceptionOrNull()?.let {
                return@withLock Result.failure(it)
            }
            val unavailable = lifecycleMutex.withLock { unavailableFailure() }
            unavailable?.let { return@withLock Result.failure(it) }
            try {
                genAI.generate(request.toGenAIRequest()).also { it.failureOrCancellation() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Result.failure(error)
            }
        }

    override fun generateFlow(request: SemanticInferenceRequest): Flow<String> = channelFlow {
        generationMutex.withLock {
            validate(request, streaming = true).getOrThrow()
            lifecycleMutex.withLock {
                unavailableFailure()?.let { throw it }
            }
            genAI.generateFlow(request.toGenAIRequest()).collect { send(it) }
        }
    }

    override fun cancel() = genAI.cancel()

    override suspend fun close(): Unit {
        closeMutex.withLock {
            var firstClose = false
            var cancelFailure: Throwable? = null
            lifecycleMutex.withLock {
                if (!closed) {
                    closed = true
                    firstClose = true
                    state.value = SemanticInferenceAvailability.Unavailable(PROVIDER_CLOSED)
                    try {
                        genAI.cancel()
                    } catch (error: Throwable) {
                        cancelFailure = error
                    }
                }
            }
            if (!firstClose) return@withLock

            var closeFailure: Throwable? = null
            withContext(NonCancellable) {
                generationMutex.withLock {
                    try {
                        genAI.close()
                    } catch (error: Throwable) {
                        closeFailure = error
                    }
                }
            }

            cancelFailure?.let { primary ->
                closeFailure?.let(primary::addSuppressed)
                throw primary
            }
            closeFailure?.let { throw it }
        }
    }

    override suspend fun markNotConfigured() =
        transition { SemanticInferenceAvailability.NotConfigured }

    override suspend fun markInitializing() =
        transition { SemanticInferenceAvailability.Initializing }

    override suspend fun markAvailable(capabilities: SemanticInferenceCapabilities) =
        transition { SemanticInferenceAvailability.Available(capabilities) }

    override suspend fun markUnavailable(reasonCode: String) =
        transition { SemanticInferenceAvailability.Unavailable(reasonCode) }

    override suspend fun markFailed(diagnosticCode: String) =
        transition { SemanticInferenceAvailability.Failed(diagnosticCode) }

    private suspend fun transition(next: () -> SemanticInferenceAvailability) {
        lifecycleMutex.withLock {
            if (!closed) state.value = next()
        }
    }

    private fun validate(request: SemanticInferenceRequest, streaming: Boolean): Result<Unit> {
        val capabilities = (state.value as? SemanticInferenceAvailability.Available)?.capabilities
            ?: return Result.failure(SemanticInferenceUnavailableException(state.value))
        if (request.prompt.isBlank()) {
            return Result.failure(InvalidSemanticInferenceRequestException())
        }
        unsupported(capabilities, SemanticInferenceCapability.TEXT_GENERATION)?.let {
            return Result.failure(it)
        }
        if (request.images.isNotEmpty()) {
            unsupported(capabilities, SemanticInferenceCapability.IMAGE_INPUT)?.let {
                return Result.failure(it)
            }
            if (request.images.any { !it.localPath.isAbsolute || !validImage(it.localPath) }) {
                return Result.failure(InvalidSemanticInferenceRequestException())
            }
        }
        if (streaming) {
            unsupported(capabilities, SemanticInferenceCapability.STREAMING)?.let {
                return Result.failure(it)
            }
        }
        return Result.success(Unit)
    }

    private fun unavailableFailure(): SemanticInferenceUnavailableException? {
        val current = state.value
        return if (!closed && current is SemanticInferenceAvailability.Available) {
            null
        } else {
            SemanticInferenceUnavailableException(current)
        }
    }

    private fun validImage(path: Path): Boolean = try {
        imageInputValidator(path)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Throwable) {
        false
    }

    private fun unsupported(
        capabilities: SemanticInferenceCapabilities,
        capability: SemanticInferenceCapability,
    ) = if (capabilities.supports(capability)) {
        null
    } else {
        UnsupportedSemanticCapabilityException(capability)
    }

    private fun <T> Result<T>.failureOrCancellation(): Throwable? = exceptionOrNull()?.also {
        if (it is CancellationException) throw it
    }

    private fun SemanticInferenceRequest.toGenAIRequest() =
        GenAIRequest(prompt, images.map { it.localPath.asFileUri() })

    private fun Path.asFileUri(): String = "file://" + toString().encodePathForUri()

    private fun String.encodePathForUri(): String {
        val digits = "0123456789ABCDEF"
        return buildString {
            encodeToByteArray().forEach { byte ->
                val value = byte.toInt() and 255
                val safe = value in 48..57 || value in 65..90 || value in 97..122 ||
                    value == 45 || value == 46 || value == 95 || value == 126 || value == 47
                if (safe) {
                    append(value.toChar())
                } else {
                    append('%')
                    append(digits[value ushr 4])
                    append(digits[value and 15])
                }
            }
        }
    }

    private companion object {
        const val PROVIDER_CLOSED = "provider_closed"
    }
}
