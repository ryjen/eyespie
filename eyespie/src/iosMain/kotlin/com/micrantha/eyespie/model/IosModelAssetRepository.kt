package com.micrantha.eyespie.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IosModelAssetRepository(
    private val capabilities: IosModelDeliveryCapabilities,
    private val transport: IosModelAssetTransport = UnconfiguredIosModelAssetTransport,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ModelAssetRepository {
    private val initialState = capabilities.snapshot().availability().initialModelAssetState()
    private val state = MutableStateFlow<ModelAssetState>(initialState)
    private var pendingArtifact: IosDownloadedArtifact? = null

    init {
        scope.launch {
            transport.observe().collect { event ->
                if (event is IosModelAssetTransportEvent.Downloaded) {
                    pendingArtifact = IosDownloadedArtifact(
                        temporaryPath = event.temporaryPath,
                        totalBytes = event.totalBytes,
                    )
                } else if (event is IosModelAssetTransportEvent.Cancelled) {
                    pendingArtifact = null
                }
                state.value = event.toModelAssetState(
                    currentState = state.value,
                    capabilityState = capabilities.snapshot().availability().initialModelAssetState(),
                )
            }
        }
    }

    override fun observe(): Flow<ModelAssetState> = state.asStateFlow()

    override suspend fun requestDownload() {
        when (capabilities.snapshot().availability()) {
            ModelDeliveryAvailability.Available -> {
                pendingArtifact = null
                state.value = ModelAssetState.Queued()
                try {
                    transport.schedule()
                } catch (error: CancellationException) {
                    pendingArtifact = null
                    state.value = capabilities.snapshot().availability().initialModelAssetState()
                    throw error
                } catch (error: IosModelAssetTransportException) {
                    state.value = ModelAssetState.Failed(
                        stage = FailureStage.Scheduling,
                        recoverable = error.recoverable,
                        diagnosticCode = error.diagnosticCode,
                    )
                } catch (_: Throwable) {
                    state.value = ModelAssetState.Failed(
                        stage = FailureStage.Scheduling,
                        recoverable = false,
                        diagnosticCode = "model_delivery_schedule_failed",
                    )
                }
            }

            ModelDeliveryAvailability.UnsupportedOperatingSystem ->
                state.value = ModelDeliveryAvailability.UnsupportedOperatingSystem.initialModelAssetState()

            ModelDeliveryAvailability.NotConfigured ->
                state.value = ModelDeliveryAvailability.NotConfigured.initialModelAssetState()
        }
    }

    override suspend fun cancelDownload() {
        transport.cancel()
        transport.removeTemporaryArtifacts()
        pendingArtifact = null
        state.value = capabilities.snapshot().availability().initialModelAssetState()
    }

    override suspend fun remove() {
        transport.cancel()
        transport.removeTemporaryArtifacts()
        pendingArtifact = null
        state.value = capabilities.snapshot().availability().initialModelAssetState()
    }

    override suspend fun resolveReadyModel(): ReadyModel? = null

    internal fun pendingDownloadedArtifact(): IosDownloadedArtifact? = pendingArtifact

    internal fun close() {
        scope.cancel()
    }
}

private fun IosModelAssetTransportEvent.toModelAssetState(
    currentState: ModelAssetState,
    capabilityState: ModelAssetState,
): ModelAssetState = when (this) {
    IosModelAssetTransportEvent.Idle -> currentState
    is IosModelAssetTransportEvent.Queued -> ModelAssetState.Queued(reason)
    is IosModelAssetTransportEvent.Downloading -> ModelAssetState.Downloading(
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
    )

    is IosModelAssetTransportEvent.Downloaded -> ModelAssetState.Verifying(
        verifiedBytes = 0,
        totalBytes = totalBytes,
    )

    is IosModelAssetTransportEvent.Failed -> ModelAssetState.Failed(
        stage = FailureStage.Download,
        recoverable = recoverable,
        diagnosticCode = diagnosticCode,
    )

    IosModelAssetTransportEvent.Cancelled -> capabilityState
}
