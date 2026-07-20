package com.micrantha.eyespie.model

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class ModelRuntimeCompatibility(
    val engine: String,
    val minimumRuntimeVersion: String,
    val minimumModelAbi: Int,
)

data class ModelAssetDescriptor(
    val id: String,
    val version: String,
    val filename: String,
    val expectedBytes: Long,
    val sha256: String,
    val runtime: ModelRuntimeCompatibility,
)

data class ReadyModel(
    val descriptor: ModelAssetDescriptor,
    val localPath: String,
)

enum class QueueReason {
    WaitingForNetwork,
    WaitingForWifi,
    WaitingForPlatformConfirmation,
    WaitingForStorage,
}

enum class FailureStage {
    Consent,
    Scheduling,
    Download,
    Verification,
    Compatibility,
    RuntimeSmokeCheck,
    Removal,
}

sealed interface ModelAssetState {
    data object NotInstalled : ModelAssetState

    data class AwaitingConsent(
        val downloadBytes: Long,
        val requiredFreeBytes: Long?,
    ) : ModelAssetState

    data class Queued(
        val reason: QueueReason? = null,
    ) : ModelAssetState

    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long?,
    ) : ModelAssetState

    data class Verifying(
        val verifiedBytes: Long,
        val totalBytes: Long,
    ) : ModelAssetState

    data class Ready(
        val version: String,
        val localPath: String,
    ) : ModelAssetState

    data class Failed(
        val stage: FailureStage,
        val recoverable: Boolean,
        val diagnosticCode: String,
    ) : ModelAssetState
}

interface ModelAssetRepository {
    fun observe(): Flow<ModelAssetState>

    suspend fun requestDownload()

    suspend fun cancelDownload()

    suspend fun remove()

    suspend fun resolveReadyModel(): ReadyModel?
}
