package com.micrantha.eyespie.model

import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

internal class PlayAssetDeliveryModelRepository(
    private val assetPackManager: AssetPackManager,
    private val descriptor: ModelAssetDescriptor,
    private val verifier: ModelAssetVerifier,
    private val runtime: ModelRuntimeCapabilities,
    verificationDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val packName: String = MODEL_PACK_NAME,
    private val assetDirectory: String = MODEL_ASSET_DIRECTORY,
) : ModelAssetRepository, AutoCloseable {
    private val state = MutableStateFlow<ModelAssetState>(
        ModelAssetState.AwaitingConsent(
            downloadBytes = descriptor.expectedBytes,
            requiredFreeBytes = null,
        ),
    )
    private val removalInProgress = AtomicBoolean(false)
    private val verificationGeneration = AtomicLong(0L)
    private val verificationScope = CoroutineScope(SupervisorJob() + verificationDispatcher)
    private var verificationJob: Job? = null
    private var verifiedCandidate: ReadyModel? = null

    private val listener = AssetPackStateUpdateListener(::handleAssetPackState)

    init {
        assetPackManager.registerListener(listener)
        if (resolveAssetFiles() != null) {
            startVerification()
        }
    }

    override fun observe(): Flow<ModelAssetState> = state.asStateFlow()

    override suspend fun requestDownload() {
        removalInProgress.set(false)

        if (resolveAssetFiles() != null) {
            startVerification()
            return
        }

        invalidateVerification()
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
        invalidateVerification()
        assetPackManager.cancel(listOf(packName))
        state.value = ModelAssetState.NotInstalled
    }

    override suspend fun remove() {
        removalInProgress.set(true)
        invalidateVerification()
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

    fun resolveCurrentAssetPath(): String? =
        resolveAssetFiles()?.modelFile?.takeIf(File::isFile)?.absolutePath

    internal fun handleAssetPackState(assetPackState: AssetPackState) {
        if (assetPackState.name() != packName || removalInProgress.get()) return

        if (assetPackState.status() == AssetPackStatus.COMPLETED) {
            startVerification()
        } else {
            invalidateVerification()
            state.value = PlayAssetDeliveryStateMapper.map(assetPackState)
        }
    }

    override fun close() {
        invalidateVerification()
        verificationScope.cancel()
        assetPackManager.unregisterListener(listener)
    }

    private fun startVerification() {
        val generation = verificationGeneration.incrementAndGet()
        verificationJob?.cancel()
        verifiedCandidate = null
        state.value = ModelAssetState.Verifying(
            verifiedBytes = 0L,
            totalBytes = descriptor.expectedBytes,
        )

        verificationJob = verificationScope.launch {
            val result = verifyInstalledAsset()
            if (generation != verificationGeneration.get() || removalInProgress.get()) return@launch
            publishVerificationResult(result)
        }
    }

    private suspend fun verifyInstalledAsset(): ModelAssetVerificationResult {
        val files = resolveAssetFiles()
        if (files == null || !files.modelFile.isFile || !files.manifestFile.isFile) {
            return ModelAssetVerificationResult.Invalid(
                stage = FailureStage.Verification,
                diagnosticCode = "verification.asset_files_missing",
            )
        }

        return verifier.verify(
            manifestPath = files.manifestFile.absolutePath.toPath(),
            modelPath = files.modelFile.absolutePath.toPath(),
            expectedDescriptor = descriptor,
            runtime = runtime,
        )
    }

    private fun publishVerificationResult(result: ModelAssetVerificationResult) {
        when (result) {
            is ModelAssetVerificationResult.Verified -> {
                verifiedCandidate = ReadyModel(
                    descriptor = result.descriptor,
                    localPath = result.localPath,
                )
                state.value = ModelAssetState.Verifying(
                    verifiedBytes = descriptor.expectedBytes,
                    totalBytes = descriptor.expectedBytes,
                )
            }

            is ModelAssetVerificationResult.Invalid -> {
                verifiedCandidate = null
                state.value = ModelAssetState.Failed(
                    stage = result.stage,
                    recoverable = true,
                    diagnosticCode = result.diagnosticCode,
                )
            }
        }
    }

    private fun invalidateVerification() {
        verificationGeneration.incrementAndGet()
        verificationJob?.cancel()
        verificationJob = null
        verifiedCandidate = null
    }

    private fun resolveAssetFiles(): AssetFiles? {
        val packLocation = assetPackManager.getPackLocation(packName) ?: return null
        val assetsPath = packLocation.assetsPath() ?: return null
        val modelDirectory = File(assetsPath, assetDirectory)
        return AssetFiles(
            manifestFile = File(modelDirectory, MODEL_MANIFEST_FILENAME),
            modelFile = File(modelDirectory, descriptor.filename),
        )
    }

    private data class AssetFiles(
        val manifestFile: File,
        val modelFile: File,
    )

    private companion object {
        const val MODEL_PACK_NAME = "model_pack"
        const val MODEL_ASSET_DIRECTORY = "model"
        const val MODEL_MANIFEST_FILENAME = "manifest.json"
    }
}
