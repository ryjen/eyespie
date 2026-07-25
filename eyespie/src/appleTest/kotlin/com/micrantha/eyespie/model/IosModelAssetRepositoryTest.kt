package com.micrantha.eyespie.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.backgroundScope
import kotlinx.coroutines.test.runTest
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosModelAssetRepositoryTest {
    @Test
    fun configuredUrlSessionMakesDeliveryAvailable() {
        assertEquals(
            ModelDeliveryAvailability.Available,
            capabilitySnapshot(urlSessionSupported = true, urlSessionConfigured = true).availability(),
        )
    }

    @Test
    fun supportedButUnconfiguredTransportFailsClosed() {
        assertEquals(
            ModelDeliveryAvailability.NotConfigured,
            capabilitySnapshot(urlSessionSupported = true).availability(),
        )
    }

    @Test
    fun defaultProductionModuleFailsClosedUntilNativeConfigurationExists() = runTest {
        val dependencies = DI { import(iosModelAssetModule()) }
        val repository = dependencies.direct.instance<ModelAssetRepository>()

        assertEquals(
            ModelAssetState.Failed(
                stage = FailureStage.Scheduling,
                recoverable = false,
                diagnosticCode = "model_delivery_not_configured",
            ),
            repository.observe().first(),
        )
        assertNull(repository.resolveReadyModel())
    }

    @Test
    fun requestSchedulesConfiguredTransportAndStartsQueued() = runTest {
        val transport = FakeIosModelAssetTransport()
        val repository = repositoryFor(transport)

        repository.requestDownload()

        assertEquals(1, transport.scheduleCalls)
        assertIs<ModelAssetState.Queued>(repository.observe().first())
        repository.close()
    }

    @Test
    fun transportProgressMapsToSharedDownloadingState() = runTest {
        val transport = FakeIosModelAssetTransport()
        val repository = repositoryFor(transport)

        transport.emit(IosModelAssetTransportEvent.Downloading(32, 128))

        assertEquals(
            ModelAssetState.Downloading(downloadedBytes = 32, totalBytes = 128),
            repository.observe().first { it is ModelAssetState.Downloading },
        )
        repository.close()
    }

    @Test
    fun completedTransferEntersVerificationButNeverReady() = runTest {
        val transport = FakeIosModelAssetTransport()
        val repository = repositoryFor(transport)

        transport.emit(IosModelAssetTransportEvent.Downloaded("/tmp/model.part", 128))

        assertEquals(
            ModelAssetState.Verifying(verifiedBytes = 0, totalBytes = 128),
            repository.observe().first { it is ModelAssetState.Verifying },
        )
        assertNull(repository.resolveReadyModel())
        repository.close()
    }

    @Test
    fun temporaryFailureIsRecoverableDownloadFailure() = runTest {
        val transport = FakeIosModelAssetTransport()
        val repository = repositoryFor(transport)

        transport.emit(IosModelAssetTransportEvent.Failed(true, "model_download_network_interrupted"))

        assertEquals(
            ModelAssetState.Failed(
                stage = FailureStage.Download,
                recoverable = true,
                diagnosticCode = "model_download_network_interrupted",
            ),
            repository.observe().first { it is ModelAssetState.Failed },
        )
        repository.close()
    }

    @Test
    fun cancellationCancelsTransferCleansTemporaryDataAndResetsState() = runTest {
        val transport = FakeIosModelAssetTransport()
        val repository = repositoryFor(transport)
        repository.requestDownload()

        repository.cancelDownload()

        assertEquals(1, transport.cancelCalls)
        assertEquals(1, transport.cleanupCalls)
        assertEquals(ModelAssetState.NotInstalled, repository.observe().first())
        repository.close()
    }

    @Test
    fun restoredQueuedEventReconnectsRepositoryState() = runTest {
        val transport = FakeIosModelAssetTransport()
        val repository = repositoryFor(transport)

        transport.emit(IosModelAssetTransportEvent.Queued(QueueReason.WaitingForNetwork))

        assertEquals(
            ModelAssetState.Queued(QueueReason.WaitingForNetwork),
            repository.observe().first { it is ModelAssetState.Queued },
        )
        repository.close()
    }

    @Test
    fun iosModuleResolvesRepositoryBehindSharedContract() {
        val dependencies = DI {
            import(
                iosModelAssetModule(
                    capabilities = capabilitiesFor(
                        capabilitySnapshot(urlSessionSupported = true, urlSessionConfigured = true),
                    ),
                    transport = FakeIosModelAssetTransport(),
                ),
            )
        }

        assertIs<IosModelAssetRepository>(dependencies.direct.instance<ModelAssetRepository>())
    }

    private fun kotlinx.coroutines.test.TestScope.repositoryFor(
        transport: IosModelAssetTransport,
    ) = IosModelAssetRepository(
        capabilities = capabilitiesFor(
            capabilitySnapshot(urlSessionSupported = true, urlSessionConfigured = true),
        ),
        transport = transport,
        scope = backgroundScope,
    )

    private fun capabilitiesFor(snapshot: IosModelDeliveryCapabilitySnapshot) =
        IosModelDeliveryCapabilities { snapshot }

    private fun capabilitySnapshot(
        urlSessionSupported: Boolean = false,
        urlSessionConfigured: Boolean = false,
        backgroundAssetsSupported: Boolean = false,
        backgroundAssetsConfigured: Boolean = false,
    ) = IosModelDeliveryCapabilitySnapshot(
        backgroundUrlSessionSupported = urlSessionSupported,
        backgroundUrlSessionConfigured = urlSessionConfigured,
        backgroundAssetsSupported = backgroundAssetsSupported,
        backgroundAssetsConfigured = backgroundAssetsConfigured,
    )
}

private class FakeIosModelAssetTransport : IosModelAssetTransport {
    private val events = MutableSharedFlow<IosModelAssetTransportEvent>(extraBufferCapacity = 8)

    var scheduleCalls = 0
    var cancelCalls = 0
    var cleanupCalls = 0

    override fun observe(): Flow<IosModelAssetTransportEvent> = events

    override suspend fun schedule() {
        scheduleCalls += 1
    }

    override suspend fun cancel() {
        cancelCalls += 1
    }

    override suspend fun removeTemporaryArtifacts() {
        cleanupCalls += 1
    }

    fun emit(event: IosModelAssetTransportEvent) {
        assertTrue(events.tryEmit(event))
    }
}
