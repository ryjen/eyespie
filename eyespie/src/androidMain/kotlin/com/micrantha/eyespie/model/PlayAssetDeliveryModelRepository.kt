package com.micrantha.eyespie.model

import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

internal class PlayAssetDeliveryModelRepository(
    private val assetPackManager: AssetPackManager,
    private val descriptor: ModelAssetDescriptor,
    private val packName: String = MODEL_PACK_NAME,
    private val assetDirectory: String = MODEL_ASSET_DIRECTORY,
) : ModelAssetRepository, AutoCloseable {
    private val state = MutableStateFlow<ModelAssetState>(initialState())

    private val listener = AssetPackStateUpdateListener { assetPackState ->
        if (assetPackState.name() == packName) {
            state.value = PlayAssetDeliveryStateMapper.map(assetPackState)
        }
    }

    init {
        assetPackManager.registerListener(listener)
    }

    override fun observe(): Flow<ModelAssetState> = state.asStateFlow()

    override suspend fun requestDownload() {
        if (resolveCurrentAssetPath() != null) {
            state.value = ModelAssetState.Verifying(
                verifiedBytes = 0L,
                totalBytes = descriptor.expectedBytes,
            )
            return
        }

        state.value = ModelAssetState.Queued()
        assetPackManager.fetch(listOf(packName))
            .addOnFailureListener { error ->
                state.value = ModelAssetState.Failed(
                    stage = FailureStage.Scheduling,
                    recoverable = true,
                    diagnosticCode = "pad.fetch_failed.${error.javaClass.simpleName}",
                )
            }
    }

    override suspend fun cancelDownload() {
        assetPackManager.cancel(listOf(packName))
        state.value = ModelAssetState.NotInstalled
    }

    override suspend fun remove() {
        state.value = ModelAssetState.NotInstalled
        assetPackManager.removePack(packName)
            .addOnFailureListener { error ->
                state.value = ModelAssetState.Failed(
                    stage = FailureStage.Removal,
                    recoverable = true,
                    diagnosticCode = "pad.remove_failed.${error.javaClass.simpleName}",
                )
            }
    }

    override suspend fun resolveReadyModel(): ReadyModel? = null

    fun resolveCurrentAssetPath(): String? {
        val packLocation = assetPackManager.getPackLocation(packName) ?: return null
        val assetsPath = packLocation.assetsPath() ?: return null
        val modelFile = File(File(assetsPath, assetDirectory), descriptor.filename)
        return modelFile.takeIf(File::isFile)?.absolutePath
    }

    override fun close() {
        assetPackManager.unregisterListener(listener)
    }

    private fun initialState(): ModelAssetState =
        if (resolveCurrentAssetPath() == null) {
            ModelAssetState.AwaitingConsent(
                downloadBytes = descriptor.expectedBytes,
                requiredFreeBytes = null,
            )
        } else {
            ModelAssetState.Verifying(
                verifiedBytes = 0L,
                totalBytes = descriptor.expectedBytes,
            )
        }

    private companion object {
        const val MODEL_PACK_NAME = "model_pack"
        const val MODEL_ASSET_DIRECTORY = "model"
    }
}
