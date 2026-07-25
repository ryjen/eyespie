package com.micrantha.eyespie.model

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.coroutines.cancellation.CancellationException

/**
 * Narrow bridge implemented by the native iOS layer.
 *
 * The bridge owns URLSession lifecycle and app-owned staging cleanup. Kotlin maps transport
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
    cause: Throwable? = null,
) : Exception(diagnosticCode, cause)

internal data class IosDownloadedArtifact(
    /** App-owned staging path. It is internal and is not exposed through shared public state. */
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

    /** Transfer completed into app-owned staging; verification is intentionally not included. */
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

/**
 * Kotlin-owned event stream used by the Swift transport implementation.
 *
 * Swift owns URLSession and filesystem behavior; this class only avoids requiring Swift to
 * construct kotlinx.coroutines Flow implementations or Kotlin sealed-event subclasses directly.
 */
class IosModelAssetTransportEventStream {
    private val events = MutableSharedFlow<IosModelAssetTransportEvent>(
        replay = 1,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun observe(): Flow<IosModelAssetTransportEvent> = events.asSharedFlow()

    fun emitIdle() {
        events.tryEmit(IosModelAssetTransportEvent.Idle)
    }

    fun emitQueued() {
        events.tryEmit(IosModelAssetTransportEvent.Queued())
    }

    fun emitWaitingForNetwork() {
        events.tryEmit(IosModelAssetTransportEvent.Queued(QueueReason.WaitingForNetwork))
    }

    fun emitDownloading(
        downloadedBytes: Long,
        totalBytes: Long,
    ) {
        events.tryEmit(IosModelAssetTransportEvent.Downloading(downloadedBytes, totalBytes))
    }

    fun emitDownloadingUnknownTotal(downloadedBytes: Long) {
        events.tryEmit(IosModelAssetTransportEvent.Downloading(downloadedBytes, null))
    }

    fun emitDownloaded(
        temporaryPath: String,
        totalBytes: Long,
    ) {
        events.tryEmit(IosModelAssetTransportEvent.Downloaded(temporaryPath, totalBytes))
    }

    fun emitFailed(
        recoverable: Boolean,
        diagnosticCode: String,
    ) {
        events.tryEmit(IosModelAssetTransportEvent.Failed(recoverable, diagnosticCode))
    }

    fun emitCancelled() {
        events.tryEmit(IosModelAssetTransportEvent.Cancelled)
    }
}

internal object UnconfiguredIosModelAssetTransport : IosModelAssetTransport {
    override fun observe(): Flow<IosModelAssetTransportEvent> =
        kotlinx.coroutines.flow.flowOf(IosModelAssetTransportEvent.Idle)

    override suspend fun schedule() = Unit

    override suspend fun cancel() = Unit

    override suspend fun removeTemporaryArtifacts() = Unit
}
