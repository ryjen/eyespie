package com.micrantha.eyespie.model

import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.assetpacks.AssetPackManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayAssetDeliveryModelRepositoryTest {
    @Test
    fun removeCancelsActiveRequestBeforeRemovingInstalledPack() = runTest {
        val assetPackManager = mockk<AssetPackManager>(relaxed = true) {
            every { getPackLocation(PACK_NAME) } returns null
            every { cancel(listOf(PACK_NAME)) } returns emptyList()
            every { removePack(PACK_NAME) } returns Tasks.forResult<Void>(null)
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
        assertEquals(ModelAssetState.NotInstalled, repository.observe().value())
        repository.close()
    }

    private suspend fun kotlinx.coroutines.flow.Flow<ModelAssetState>.value(): ModelAssetState =
        kotlinx.coroutines.flow.first(this)

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
