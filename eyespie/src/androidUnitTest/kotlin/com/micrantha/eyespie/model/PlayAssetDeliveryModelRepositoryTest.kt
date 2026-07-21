package com.micrantha.eyespie.model

import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.assetpacks.AssetPackLocation
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStates
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayAssetDeliveryModelRepositoryTest {
    private val temporaryDirectories = mutableListOf<Path>()

    @AfterTest
    fun cleanUp() {
        temporaryDirectories.forEach { it.toFile().deleteRecursively() }
    }

    @Test
    fun installedPackIsVerifiedButNotExposedAsReadyBeforeRuntimeSmokeCheck() = runTest {
        val assetsPath = writeArtifact()
        val assetPackManager = assetPackManagerWithLocation(assetsPath)
        val repository = repository(assetPackManager)

        assertEquals(
            ModelAssetState.Verifying(
                verifiedBytes = descriptor().expectedBytes,
                totalBytes = descriptor().expectedBytes,
            ),
            repository.observe().first(),
        )
        assertNull(repository.resolveReadyModel())
        repository.close()
    }

    @Test
    fun corruptInstalledPackFailsVerificationAndIsNeverReady() = runTest {
        val assetsPath = writeArtifact(modelContent = "different payload!")
        val repository = repository(assetPackManagerWithLocation(assetsPath))

        assertEquals(
            ModelAssetState.Failed(
                stage = FailureStage.Verification,
                recoverable = true,
                diagnosticCode = "verification.digest_mismatch",
            ),
            repository.observe().first(),
        )
        assertNull(repository.resolveReadyModel())
        repository.close()
    }

    @Test
    fun completedUpdateReResolvesCurrentPackLocationBeforeVerification() = runTest {
        var assetsPath: String? = null
        val location = mockk<AssetPackLocation> {
            every { assetsPath() } answers { assetsPath }
        }
        val assetPackManager = mockk<AssetPackManager>(relaxed = true) {
            every { getPackLocation(PACK_NAME) } answers {
                if (assetsPath == null) null else location
            }
        }
        val repository = repository(assetPackManager)
        assetsPath = writeArtifact()

        repository.handleAssetPackState(completedState())

        assertEquals(
            ModelAssetState.Verifying(
                verifiedBytes = descriptor().expectedBytes,
                totalBytes = descriptor().expectedBytes,
            ),
            repository.observe().first(),
        )
        verify(atLeast = 2) { assetPackManager.getPackLocation(PACK_NAME) }
        repository.close()
    }

    @Test
    fun missingManifestAfterCompletedUpdateFailsVerification() = runTest {
        val root = newAssetsDirectory()
        val modelDirectory = root.resolve("model").createDirectories()
        modelDirectory.resolve(MODEL_FILENAME).writeText(MODEL_CONTENT)
        val repository = repository(assetPackManagerWithLocation(root.toString()))

        assertEquals(
            ModelAssetState.Failed(
                stage = FailureStage.Verification,
                recoverable = true,
                diagnosticCode = "verification.asset_files_missing",
            ),
            repository.observe().first(),
        )
        repository.close()
    }

    @Test
    fun removeCancelsActiveRequestBeforeRemovingInstalledPack() = runTest {
        val completedRemoval = successfulVoidTask()
        val assetPackManager = mockk<AssetPackManager>(relaxed = true) {
            every { getPackLocation(PACK_NAME) } returns null
            every { cancel(listOf(PACK_NAME)) } returns mockk<AssetPackStates>()
            every { removePack(PACK_NAME) } returns completedRemoval
        }
        val repository = repository(assetPackManager)

        repository.remove()

        verifyOrder {
            assetPackManager.cancel(listOf(PACK_NAME))
            assetPackManager.removePack(PACK_NAME)
        }
        assertEquals(ModelAssetState.NotInstalled, repository.observe().first())
        repository.close()
    }

    @Test
    fun removeIgnoresLateDownloadUpdatesUntilRemovalCompletes() = runTest {
        val pendingRemoval = mockk<Task<Void>>(relaxed = true)
        val assetPackManager = mockk<AssetPackManager>(relaxed = true) {
            every { getPackLocation(PACK_NAME) } returns null
            every { cancel(listOf(PACK_NAME)) } returns mockk<AssetPackStates>()
            every { removePack(PACK_NAME) } returns pendingRemoval
        }
        val repository = repository(assetPackManager)

        repository.remove()
        repository.handleAssetPackState(
            mockk<AssetPackState> {
                every { name() } returns PACK_NAME
                every { status() } returns AssetPackStatus.DOWNLOADING
                every { bytesDownloaded() } returns 20L
                every { totalBytesToDownload() } returns 39L
            },
        )

        assertEquals(
            ModelAssetState.AwaitingConsent(downloadBytes = 18L, requiredFreeBytes = null),
            repository.observe().first(),
        )
        repository.close()
    }

    private fun repository(assetPackManager: AssetPackManager) =
        PlayAssetDeliveryModelRepository(
            assetPackManager = assetPackManager,
            descriptor = descriptor(),
            verifier = ModelAssetVerifier(FileSystem.SYSTEM),
            runtime = ModelRuntimeCapabilities(
                engine = "mediapipe",
                version = "0.10.35",
                modelAbi = 1,
            ),
            packName = PACK_NAME,
        )

    private fun assetPackManagerWithLocation(assetsPath: String): AssetPackManager {
        val location = mockk<AssetPackLocation> {
            every { assetsPath() } returns assetsPath
        }
        return mockk(relaxed = true) {
            every { getPackLocation(PACK_NAME) } returns location
        }
    }

    private fun completedState() = mockk<AssetPackState> {
        every { name() } returns PACK_NAME
        every { status() } returns AssetPackStatus.COMPLETED
        every { totalBytesToDownload() } returns 18L
    }

    private fun writeArtifact(
        manifest: String = MANIFEST,
        modelContent: String = MODEL_CONTENT,
    ): String {
        val root = newAssetsDirectory()
        val modelDirectory = root.resolve("model").createDirectories()
        modelDirectory.resolve("manifest.json").writeText(manifest)
        modelDirectory.resolve(MODEL_FILENAME).writeText(modelContent)
        return root.toString()
    }

    private fun newAssetsDirectory(): Path =
        Files.createTempDirectory("eyespie-pad-test").also(temporaryDirectories::add)

    private fun successfulVoidTask(): Task<Void> {
        val task = mockk<Task<Void>>()
        every { task.addOnSuccessListener(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val listener = invocation.args[0] as OnSuccessListener<Void>
            listener.onSuccess(null)
            task
        }
        every { task.addOnFailureListener(any()) } returns task
        return task
    }

    private fun descriptor() = ModelAssetDescriptor(
        id = "eyespie-offline-model-smoke-test",
        version = VERSION,
        filename = MODEL_FILENAME,
        expectedBytes = 18L,
        sha256 = MODEL_DIGEST,
        runtime = ModelRuntimeCompatibility(
            engine = "mediapipe",
            minimumRuntimeVersion = "0.10.35",
            minimumModelAbi = 1,
        ),
    )

    private companion object {
        const val PACK_NAME = "model_pack"
        const val MODEL_FILENAME = "pad-smoke-test.bin"
        const val MODEL_CONTENT = "test model payload"
        const val MODEL_DIGEST = "bae77ae8633e61e7906d62148fecbf0f322507fe9b145afb5e3081af6b0e8b88"
        const val VERSION = "pad-smoke-2026-07-20.1"
        val MANIFEST = """
            {
              "schemaVersion": 1,
              "modelId": "eyespie-offline-model-smoke-test",
              "version": "$VERSION",
              "filename": "$MODEL_FILENAME",
              "sizeBytes": 18,
              "sha256": "$MODEL_DIGEST",
              "runtime": {
                "engine": "mediapipe",
                "minimumRuntimeVersion": "0.10.35",
                "minimumModelAbi": 1
              }
            }
        """.trimIndent()
    }
}
