package com.micrantha.eyespie.model

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class IosModelAssetRepositoryTest {
    @Test
    fun configuredUrlSessionMakesDeliveryAvailable() {
        val snapshot = capabilitySnapshot(
            urlSessionSupported = true,
            urlSessionConfigured = true,
        )

        assertEquals(ModelDeliveryAvailability.Available, snapshot.availability())
    }

    @Test
    fun supportedButUnconfiguredTransportFailsClosed() {
        val snapshot = capabilitySnapshot(
            urlSessionSupported = true,
            urlSessionConfigured = false,
        )

        assertEquals(ModelDeliveryAvailability.NotConfigured, snapshot.availability())
    }

    @Test
    fun noSupportedTransportReportsUnsupportedOperatingSystem() {
        assertEquals(
            ModelDeliveryAvailability.UnsupportedOperatingSystem,
            capabilitySnapshot().availability(),
        )
    }

    @Test
    fun configuredBackgroundAssetsIsViableWithoutUrlSession() {
        val snapshot = capabilitySnapshot(
            backgroundAssetsSupported = true,
            backgroundAssetsConfigured = true,
        )

        assertEquals(ModelDeliveryAvailability.Available, snapshot.availability())
    }

    @Test
    fun availableRepositoryStartsNotInstalledAndNeverResolvesReadyModel() = runTest {
        val repository = repositoryFor(
            capabilitySnapshot(
                urlSessionSupported = true,
                urlSessionConfigured = true,
            ),
        )

        assertEquals(ModelAssetState.NotInstalled, repository.observe().first())
        assertNull(repository.resolveReadyModel())
    }

    @Test
    fun requestDownloadFailsClosedUntilTransportExists() = runTest {
        val repository = repositoryFor(
            capabilitySnapshot(
                urlSessionSupported = true,
                urlSessionConfigured = true,
            ),
        )

        repository.requestDownload()

        assertEquals(
            ModelAssetState.Failed(
                stage = FailureStage.Scheduling,
                recoverable = false,
                diagnosticCode = "model_delivery_transport_not_implemented",
            ),
            repository.observe().first(),
        )
        assertNull(repository.resolveReadyModel())
    }

    @Test
    fun removeReturnsRepositoryToCurrentCapabilityState() = runTest {
        val repository = repositoryFor(
            capabilitySnapshot(
                urlSessionSupported = true,
                urlSessionConfigured = true,
            ),
        )
        repository.requestDownload()

        repository.remove()

        assertEquals(ModelAssetState.NotInstalled, repository.observe().first())
    }

    @Test
    fun iosModuleResolvesRepositoryBehindSharedContract() {
        val dependencies = DI {
            import(
                iosModelAssetModule(
                    capabilitiesFor(
                        capabilitySnapshot(
                            urlSessionSupported = true,
                            urlSessionConfigured = true,
                        ),
                    ),
                ),
            )
        }

        assertIs<IosModelAssetRepository>(
            dependencies.direct.instance<ModelAssetRepository>(),
        )
    }

    private fun repositoryFor(snapshot: IosModelDeliveryCapabilitySnapshot) =
        IosModelAssetRepository(capabilitiesFor(snapshot))

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
