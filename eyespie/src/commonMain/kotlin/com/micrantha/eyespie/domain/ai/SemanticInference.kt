package com.micrantha.eyespie.domain.ai

import kotlinx.coroutines.flow.Flow

enum class InferenceLocality { LOCAL, REMOTE }

data class SemanticInferenceCapabilities(
    val textGeneration: Boolean,
    val imageInput: Boolean,
    val streaming: Boolean,
    val maxContextTokens: Int? = null,
)

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
    data class Unavailable(val reason: String) : SemanticInferenceAvailability
    data class Failed(val diagnosticCode: String) : SemanticInferenceAvailability
}

data class SemanticInferenceRequest(
    val prompt: String,
    val images: List<String> = emptyList(),
)

interface SemanticInferenceProvider {
    val identity: SemanticInferenceIdentity
    val availability: SemanticInferenceAvailability
    fun generate(request: SemanticInferenceRequest): Result<String>
    fun generateFlow(request: SemanticInferenceRequest): Flow<String>
    fun cancel()
    fun close()
}

interface SemanticInferenceAvailabilityController {
    fun markNotConfigured()
    fun markInitializing()
    fun markAvailable(capabilities: SemanticInferenceCapabilities)
    fun markUnavailable(reason: String)
    fun markFailed(diagnosticCode: String)
}

class SemanticInferenceUnavailableException(
    val state: SemanticInferenceAvailability,
) : IllegalStateException("semantic inference provider is not available: $state")

class UnsupportedSemanticCapabilityException(
    capability: String,
) : IllegalArgumentException("semantic inference provider does not support $capability")
