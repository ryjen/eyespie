package com.micrantha.eyespie.model

import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

internal class PlayAssetDeliveryModelRepository(
    private val assetPackManager: AssetPackManager,
    private val descriptor: ModelAssetDescriptor,
    private val packName: String = MODEL_PACK_NAME,
    private val assetDirectory: String = MODEL_ASSET_DIRECTORY,
) : ModelAssetRepository, AutoCloseable {
    private val state = MutableStateFlow<ModelAssetState>(initialState())
    private val removalInProgress = AtomicBoolean(false)

    private val listener = AssetPackStateUpdateListener(::handleAssetPackState)

    init {
        assetPackManager.registerListener(listener)
    }

    override fun observe(): Flow<ModelAssetState> = state.asStateFlow()

    override suspend fun requestDownload() {
        removalInProgress.set(false)

        if (resolveCurrentAssetPath() != null) {
            state.value = ModelAssetState.Verifying(
                verifiedBytes = 0L,
                totalBytes = descriptor.expectedBytes,
            )
            return
        }

        state.value = ModelAssetState.Queued()
        assetPackManager.fetch(listOf(packName))
            .addOnFailureListener {
                state.value = ModelAssetState.Failed(
                    stage = FailureStage.Scheduling,
                    recoverable = true,
                    diagnosticCode = "pad.fetch_failed",
                )
            }
    }

    override suspend fun cancelDownload() {
        assetPackManager.cancel(listOf(packName))
        state.value = ModelAssetState.NotInstalled
    }

    override suspend fun remove() {
        removalInProgress.set(true)
        assetPackManager.cancel(listOf(packName))
        assetPackManager.removePack(packName)
            .addOnSuccessListener {
                state.value = ModelAssetState.NotInstalled
            }
            .addOnFailureListener {
                removalInProgress.set(false)
                state.value = ModelAssetState.Failed(
                    stage = FailureStage.Removal,
                    recoverable = true,
                    diagnosticCode = "pad.remove_failed",
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

    internal fun handleAssetPackState(assetPackState: AssetPackState) {
        if (assetPackState.name() == packName && !removalInProgress.get()) {
            state.value = PlayAssetDeliveryStateMapper.map(assetPackState)
        }
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
