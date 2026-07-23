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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toPath
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

internal class PlayAssetDeliveryModelRepository(
    private val assetPackManager: AssetPackManager,
    private val descriptor: ModelAssetDescriptor,
    private val verifier: ModelAssetVerifier,
    private val runtime: ModelRuntimeCapabilities,
    private val smokeChecker: ModelRuntimeSmokeChecker,
    verificationDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val smokeCheckTimeoutMillis: Long = DEFAULT_SMOKE_CHECK_TIMEOUT_MILLIS,
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
    private var readyModel: ReadyModel? = null

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

    override suspend fun resolveReadyModel(): ReadyModel? = readyModel

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
        readyModel = null
        state.value = ModelAssetState.Verifying(
            verifiedBytes = 0L,
            totalBytes = descriptor.expectedBytes,
        )

        verificationJob = verificationScope.launch {
            when (val result = verifyInstalledAsset()) {
                is ModelAssetVerificationResult.Invalid -> {
                    if (!isCurrent(generation)) return@launch
                    publishVerificationFailure(result)
                }

                is ModelAssetVerificationResult.Verified -> {
                    if (!isCurrent(generation)) return@launch
                    val candidate = ReadyModel(
                        descriptor = result.descriptor,
                        localPath = result.localPath,
                    )
                    state.value = ModelAssetState.Verifying(
                        verifiedBytes = descriptor.expectedBytes,
                        totalBytes = descriptor.expectedBytes,
                    )
                    val smokeResult = runSmokeCheck(candidate)
                    if (!isCurrent(generation)) return@launch
                    publishSmokeCheckResult(candidate, smokeResult)
                }
            }
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

    private suspend fun runSmokeCheck(candidate: ReadyModel): RuntimeSmokeCheckResult = try {
        withTimeout(smokeCheckTimeoutMillis) {
            smokeChecker.check(candidate)
        }
    } catch (_: TimeoutCancellationException) {
        RuntimeSmokeCheckResult.Failed(
            recoverable = true,
            diagnosticCode = "runtime.timeout",
        )
    }

    private fun publishVerificationFailure(result: ModelAssetVerificationResult.Invalid) {
        readyModel = null
        state.value = ModelAssetState.Failed(
            stage = result.stage,
            recoverable = true,
            diagnosticCode = result.diagnosticCode,
        )
    }

    private fun publishSmokeCheckResult(
        candidate: ReadyModel,
        result: RuntimeSmokeCheckResult,
    ) {
        when (result) {
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
                    recoverable = result.recoverable,
                    diagnosticCode = result.diagnosticCode,
                )
            }
        }
    }

    private fun isCurrent(generation: Long): Boolean =
        generation == verificationGeneration.get() && !removalInProgress.get()

    private fun invalidateVerification() {
        verificationGeneration.incrementAndGet()
        verificationJob?.cancel()
        verificationJob = null
        readyModel = null
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
        const val DEFAULT_SMOKE_CHECK_TIMEOUT_MILLIS = 15_000L
    }
}
