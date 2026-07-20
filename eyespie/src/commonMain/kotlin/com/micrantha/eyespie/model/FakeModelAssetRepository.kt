package com.micrantha.eyespie.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Deterministic repository for common tests, previews, and onboarding development. */
class FakeModelAssetRepository(
    initialState: ModelAssetState = ModelAssetState.NotInstalled,
    initialReadyModel: ReadyModel? = null,
) : ModelAssetRepository {
    private var readyModel: ReadyModel? = initialReadyModel
    private val state = MutableStateFlow(initialState)

    init {
        require(initialState !is ModelAssetState.Ready || initialReadyModel != null) {
            "Ready state requires a resolved model"
        }
        require(initialState is ModelAssetState.Ready || initialReadyModel == null) {
            "Resolved model requires Ready state"
        }
    }

    override fun observe(): Flow<ModelAssetState> = state.asStateFlow()

    override suspend fun requestDownload() {
        val current = state.value
        state.value = when (current) {
            is ModelAssetState.AwaitingConsent,
            ModelAssetState.NotInstalled,
            -> ModelAssetState.Queued()

            is ModelAssetState.Failed ->
                if (current.recoverable) ModelAssetState.Queued() else current

            else -> current
        }
    }

    override suspend fun cancelDownload() {
        when (state.value) {
            is ModelAssetState.Queued,
            is ModelAssetState.Downloading,
            is ModelAssetState.Verifying,
            -> {
                readyModel = null
                state.value = ModelAssetState.NotInstalled
            }

            else -> Unit
        }
    }

    override suspend fun remove() {
        readyModel = null
        state.value = ModelAssetState.NotInstalled
    }

    override suspend fun resolveReadyModel(): ReadyModel? =
        if (state.value is ModelAssetState.Ready) readyModel else null

    fun emit(next: ModelAssetState, model: ReadyModel? = readyModel) {
        require(next !is ModelAssetState.Ready || model != null) {
            "Ready state requires a resolved model"
        }
        require(next is ModelAssetState.Ready || model == null) {
            "Resolved model requires Ready state"
        }

        readyModel = model
        state.value = next
    }
}
