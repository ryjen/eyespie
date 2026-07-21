package com.micrantha.eyespie.model

import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStates
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayAssetDeliveryModelRepositoryTest {
    @Test
    fun removeCancelsActiveRequestBeforeRemovingInstalledPack() = runTest {
        val completedRemoval = successfulVoidTask()
        val assetPackManager = mockk<AssetPackManager>(relaxed = true) {
            every { getPackLocation(PACK_NAME) } returns null
            every { cancel(listOf(PACK_NAME)) } returns mockk<AssetPackStates>()
            every { removePack(PACK_NAME) } returns completedRemoval
        }
        val repository = PlayAssetDeliveryModelRepository(
            assetPackManager = assetPackManager,
            descriptor = descriptor(),
            packName = PACK_NAME,
        )

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
        val repository = PlayAssetDeliveryModelRepository(
            assetPackManager = assetPackManager,
            descriptor = descriptor(),
            packName = PACK_NAME,
        )

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
            ModelAssetState.AwaitingConsent(downloadBytes = 39L, requiredFreeBytes = null),
            repository.observe().first(),
        )
        repository.close()
    }

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
        version = "pad-smoke-2026-07-20.1",
        filename = "pad-smoke-test.bin",
        expectedBytes = 39L,
        sha256 = "a9be80b9c833cb32e1a41ae404bdebf1b5d74ab2e85d8ca6ad44e5fd7e824ddf",
        runtime = ModelRuntimeCompatibility(
            engine = "mediapipe",
            minimumRuntimeVersion = "0.10.35",
            minimumModelAbi = 1,
        ),
    )

    private companion object {
        const val PACK_NAME = "model_pack"
    }
}
