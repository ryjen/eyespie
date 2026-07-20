package com.micrantha.eyespie.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Deterministic repository for common tests, previews, and onboarding development. */
class FakeModelAssetRepository(
    initialState: ModelAssetState = ModelAssetState.NotInstalled,
    private var readyModel: ReadyModel? = null,
) : ModelAssetRepository {
    private val state = MutableStateFlow(initialState)

    override fun observe(): Flow<ModelAssetState> = state.asStateFlow()

    override suspend fun requestDownload() {
        val current = state.value
        state.value = when (current) {
            is ModelAssetState.AwaitingConsent,
            ModelAssetState.NotInstalled,
            is ModelAssetState.Failed,
            -> ModelAssetState.Queued()

            else -> current
        }
    }

    override suspend fun cancelDownload() {
        state.value = ModelAssetState.NotInstalled
        readyModel = null
    }

    override suspend fun remove() {
        state.value = ModelAssetState.NotInstalled
        readyModel = null
    }

    override suspend fun resolveReadyModel(): ReadyModel? =
        if (state.value is ModelAssetState.Ready) readyModel else null

    fun emit(next: ModelAssetState, model: ReadyModel? = readyModel) {
        require(next !is ModelAssetState.Ready || model != null) {
            "Ready state requires a resolved model"
        }
        state.value = next
        readyModel = if (next is ModelAssetState.Ready) model else null
    }
}
