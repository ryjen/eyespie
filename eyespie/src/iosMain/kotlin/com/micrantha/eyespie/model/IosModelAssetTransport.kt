package com.micrantha.eyespie.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Narrow bridge implemented by the native iOS layer.
 *
 * The bridge owns URLSession lifecycle and temporary-file cleanup. Kotlin maps transport
 * observations into the shared model lifecycle and never treats transfer completion as Ready.
 */
interface IosModelAssetTransport {
    fun observe(): Flow<IosModelAssetTransportEvent>

    @Throws(IosModelAssetTransportException::class, CancellationException::class)
    suspend fun schedule()

    suspend fun cancel()

    suspend fun removeTemporaryArtifacts()
}

class IosModelAssetTransportException(
    val recoverable: Boolean,
    val diagnosticCode: String,
) : Exception(diagnosticCode)

internal data class IosDownloadedArtifact(
    val temporaryPath: String,
    val totalBytes: Long,
)

sealed interface IosModelAssetTransportEvent {
    data object Idle : IosModelAssetTransportEvent

    data class Queued(
        val reason: QueueReason? = null,
    ) : IosModelAssetTransportEvent

    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long?,
    ) : IosModelAssetTransportEvent

    /** Transfer completed into a temporary location; verification is intentionally not included. */
    data class Downloaded(
        val temporaryPath: String,
        val totalBytes: Long,
    ) : IosModelAssetTransportEvent

    data class Failed(
        val recoverable: Boolean,
        val diagnosticCode: String,
    ) : IosModelAssetTransportEvent

    data object Cancelled : IosModelAssetTransportEvent
}

internal object UnconfiguredIosModelAssetTransport : IosModelAssetTransport {
    override fun observe(): Flow<IosModelAssetTransportEvent> =
        kotlinx.coroutines.flow.flowOf(IosModelAssetTransportEvent.Idle)

    override suspend fun schedule() = Unit

    override suspend fun cancel() = Unit

    override suspend fun removeTemporaryArtifacts() = Unit
}
