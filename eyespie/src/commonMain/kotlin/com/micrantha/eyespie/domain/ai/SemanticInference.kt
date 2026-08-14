package com.micrantha.eyespie.domain.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import okio.Path

enum class InferenceLocality { LOCAL, REMOTE }

enum class SemanticInferenceCapability {
    TEXT_GENERATION,
    IMAGE_INPUT,
    STREAMING,
    CANCELLATION,
}

data class SemanticInferenceCapabilities(
    val textGeneration: Boolean,
    val imageInput: Boolean,
    val streaming: Boolean,
    val cancellation: Boolean = false,
    val maxContextTokens: Int? = null,
) {
    fun supports(capability: SemanticInferenceCapability) = when (capability) {
        SemanticInferenceCapability.TEXT_GENERATION -> textGeneration
        SemanticInferenceCapability.IMAGE_INPUT -> imageInput
        SemanticInferenceCapability.STREAMING -> streaming
        SemanticInferenceCapability.CANCELLATION -> cancellation
    }
}

data class SemanticInferenceIdentity(
    val providerId: String,
    val runtimeId: String,
    val locality: InferenceLocality,
    val modelId: String? = null,
    val modelVersion: String? = null,
)

sealed interface SemanticInferenceAvailability {
    data object NotConfigured : SemanticInferenceAvailability
    data object Initializing : SemanticInferenceAvailability
    data class Available(val capabilities: SemanticInferenceCapabilities) : SemanticInferenceAvailability
    data class Unavailable(val reasonCode: String) : SemanticInferenceAvailability
    data class Failed(val diagnosticCode: String) : SemanticInferenceAvailability
}

object SemanticInferenceReasonCode {
    const val PLATFORM_IMAGE_INPUT_UNSUPPORTED = "platform_image_input_unsupported"
    const val MODEL_NOT_CONFIGURED = "model_not_configured"
    const val MODEL_UNAVAILABLE = "model_unavailable"
}

object SemanticInferenceDiagnosticCode {
    const val MODEL_INTEGRITY_FAILED = "model_integrity_failed"
    const val RUNTIME_INITIALIZATION_FAILED = "runtime_initialization_failed"
}

data class SemanticInferenceSamplingConfiguration(
    val topK: Int,
    val topP: Float,
    val temperature: Float,
    val randomSeed: Int,
)

data class SemanticInferenceInitialization(
    val modelPath: Path,
    val identity: SemanticInferenceIdentity,
    val capabilities: SemanticInferenceCapabilities,
    val maxImages: Int,
    val sampling: SemanticInferenceSamplingConfiguration,
)

data class SemanticImageInput(val localPath: Path)

data class SemanticInferenceRequest(
    val prompt: String,
    val images: List<SemanticImageInput> = emptyList(),
)

interface SemanticInferenceProvider {
    val identity: SemanticInferenceIdentity
    val availability: StateFlow<SemanticInferenceAvailability>

    suspend fun generate(request: SemanticInferenceRequest): Result<String>
    fun generateFlow(request: SemanticInferenceRequest): Flow<String>
    fun cancel()
    suspend fun close()
}

/**
 * Application-owned setup boundary for a selected semantic provider.
 *
 * Implementations may validate/configure a runtime here, but logical request sessions remain owned
 * by the runtime adapter and must not be retained across independent requests.
 */
interface SemanticInferenceProviderSetup {
    suspend fun initialize(configuration: SemanticInferenceInitialization): Result<Unit>
}

interface SemanticInferenceAvailabilityController {
    suspend fun markNotConfigured()
    suspend fun markInitializing()
    suspend fun markAvailable(capabilities: SemanticInferenceCapabilities)

    suspend fun markAvailable(
        capabilities: SemanticInferenceCapabilities,
        identity: SemanticInferenceIdentity,
    ) = markAvailable(capabilities)

    suspend fun markUnavailable(reasonCode: String)
    suspend fun markFailed(diagnosticCode: String)
}

class SemanticInferenceUnavailableException(val state: SemanticInferenceAvailability) :
    IllegalStateException("semantic inference provider is not available")

class UnsupportedSemanticCapabilityException(val capability: SemanticInferenceCapability) :
    IllegalArgumentException("semantic inference provider does not support ${capability.name.lowercase()}")

class InvalidSemanticInferenceRequestException :
    IllegalArgumentException("semantic inference request is invalid")
