package com.micrantha.eyespie.model

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FakeModelAssetRepositoryTest {
    @Test
    fun consentRequestQueuesDownload() = runTest {
        val repository = FakeModelAssetRepository(
            ModelAssetState.AwaitingConsent(
                downloadBytes = 612_368_384,
                requiredFreeBytes = 700_000_000,
            ),
        )

        repository.observe().test {
            awaitItem()
            repository.requestDownload()
            assertEquals(ModelAssetState.Queued(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun readyModelIsResolvedOnlyWhileReady() = runTest {
        val descriptor = descriptor()
        val readyModel = ReadyModel(descriptor, "/runtime/model.task")
        val repository = FakeModelAssetRepository()

        repository.emit(ModelAssetState.Ready(descriptor.version, readyModel.localPath), readyModel)
        assertEquals(readyModel, repository.resolveReadyModel())

        repository.remove()
        assertNull(repository.resolveReadyModel())
        repository.observe().test {
            assertEquals(ModelAssetState.NotInstalled, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cancellationClearsPartialState() = runTest {
        val repository = FakeModelAssetRepository(
            ModelAssetState.Downloading(downloadedBytes = 100, totalBytes = 200),
        )

        repository.cancelDownload()

        repository.observe().test {
            assertEquals(ModelAssetState.NotInstalled, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(repository.resolveReadyModel())
    }

    private fun descriptor() = ModelAssetDescriptor(
        id = "eyespie-offline-model",
        version = "2026.07.20-1",
        filename = "offline-model.task",
        expectedBytes = 612_368_384,
        sha256 = "a".repeat(64),
        runtime = ModelRuntimeCompatibility("mediapipe", "0.10.26", 1),
    )
}
