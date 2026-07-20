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
        requireConsistentReadyState(initialState, initialReadyModel)
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
        requireConsistentReadyState(next, model)
        readyModel = model
        state.value = next
    }

    private fun requireConsistentReadyState(
        next: ModelAssetState,
        model: ReadyModel?,
    ) {
        if (next is ModelAssetState.Ready) {
            requireNotNull(model) { "Ready state requires a resolved model" }
            require(next.version == model.descriptor.version) {
                "Ready state version must match resolved model"
            }
            require(next.localPath == model.localPath) {
                "Ready state path must match resolved model"
            }
        } else {
            require(model == null) { "Resolved model requires Ready state" }
        }
    }
}
