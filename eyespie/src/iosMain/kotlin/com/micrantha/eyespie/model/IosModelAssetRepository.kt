package com.micrantha.eyespie.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class IosModelAssetRepository(
    private val capabilities: IosModelDeliveryCapabilities,
) : ModelAssetRepository {
    private val initialState = capabilities.snapshot().availability().initialModelAssetState()
    private val state = MutableStateFlow<ModelAssetState>(initialState)

    override fun observe(): Flow<ModelAssetState> = state.asStateFlow()

    override suspend fun requestDownload() {
        state.value = when (capabilities.snapshot().availability()) {
            ModelDeliveryAvailability.Available -> ModelAssetState.Failed(
                stage = FailureStage.Scheduling,
                recoverable = false,
                diagnosticCode = "model_delivery_transport_not_implemented",
            )

            ModelDeliveryAvailability.UnsupportedOperatingSystem ->
                ModelDeliveryAvailability.UnsupportedOperatingSystem.initialModelAssetState()

            ModelDeliveryAvailability.NotConfigured ->
                ModelDeliveryAvailability.NotConfigured.initialModelAssetState()
        }
    }

    override suspend fun cancelDownload() = Unit

    override suspend fun remove() {
        state.value = capabilities.snapshot().availability().initialModelAssetState()
    }

    override suspend fun resolveReadyModel(): ReadyModel? = null
}
