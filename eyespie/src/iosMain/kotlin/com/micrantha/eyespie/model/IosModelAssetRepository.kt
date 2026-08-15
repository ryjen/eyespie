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
import okio.Path.Companion.toPath

internal class IosModelAssetRepository(
    private val capabilities: IosModelDeliveryCapabilities,
    private val descriptor: ModelAssetDescriptor,
    private val verifier: ModelAssetVerifier,
    private val runtime: ModelRuntimeCapabilities,
    private val smokeChecker: ModelRuntimeSmokeChecker,
    private val transport: IosModelAssetTransport = UnconfiguredIosModelAssetTransport,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ModelAssetRepository {
    private val initialState = capabilities.snapshot().availability().initialModelAssetState()
    private val state = MutableStateFlow<ModelAssetState>(initialState)
    private var readyModel: ReadyModel? = null

    init {
        scope.launch {
            transport.observe().collect { event ->
                handleTransportEvent(event)
            }
        }
    }

    private suspend fun handleTransportEvent(event: IosModelAssetTransportEvent) {
        when (event) {
            is IosModelAssetTransportEvent.Downloaded -> {
                state.value = ModelAssetState.Verifying(
                    verifiedBytes = 0,
                    totalBytes = event.totalBytes,
                )
                verifyAndInstall(event.temporaryPath)
            }
            is IosModelAssetTransportEvent.Cancelled -> {
                readyModel = null
                state.value = capabilities.snapshot().availability().initialModelAssetState()
            }
            else -> {
                state.value = event.toModelAssetState(
                    currentState = state.value,
                    capabilityState = capabilities.snapshot().availability().initialModelAssetState(),
                )
            }
        }
    }

    private suspend fun verifyAndInstall(temporaryPath: String) {
        val manifestPath = temporaryPath.toPath().parent?.resolve("manifest.json") ?: "manifest.json".toPath()
        
        val result = verifier.verify(
            manifestPath = manifestPath,
            modelPath = temporaryPath.toPath(),
            expectedDescriptor = descriptor,
            runtime = runtime
        )

        when (result) {
            is ModelAssetVerificationResult.Verified -> {
                val candidate = ReadyModel(
                    descriptor = result.descriptor,
                    localPath = result.localPath,
                )
                
                state.value = ModelAssetState.Verifying(
                    verifiedBytes = descriptor.expectedBytes,
                    totalBytes = descriptor.expectedBytes,
                )

                val smokeResult = smokeChecker.check(candidate)
                
                when (smokeResult) {
                    RuntimeSmokeCheckResult.Passed -> {
                        readyModel = candidate
                        state.value = ModelAssetState.Ready(
                            version = candidate.descriptor.version,
                            localPath = candidate.localPath,
                        )
                    }
                    is RuntimeSmokeCheckResult.Failed -> {
                        readyModel = null
                        state.value = ModelAssetState.Failed(
                            stage = FailureStage.RuntimeSmokeCheck,
                            recoverable = smokeResult.recoverable,
                            diagnosticCode = smokeResult.diagnosticCode,
                        )
                    }
                }
            }
            is ModelAssetVerificationResult.Invalid -> {
                readyModel = null
                state.value = ModelAssetState.Failed(
                    stage = result.stage,
                    recoverable = true,
                    diagnosticCode = result.diagnosticCode,
                )
            }
        }
    }

    override fun observe(): Flow<ModelAssetState> = state.asStateFlow()

    override suspend fun requestDownload() {
        when (capabilities.snapshot().availability()) {
            ModelDeliveryAvailability.Available -> {
                readyModel = null
                state.value = ModelAssetState.Queued()
                try {
                    transport.schedule()
                } catch (error: CancellationException) {
                    readyModel = null
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
        readyModel = null
        state.value = capabilities.snapshot().availability().initialModelAssetState()
    }

    override suspend fun remove() {
        transport.cancel()
        transport.removeTemporaryArtifacts()
        readyModel = null
        state.value = capabilities.snapshot().availability().initialModelAssetState()
    }

    override suspend fun resolveReadyModel(): ReadyModel? = readyModel

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
