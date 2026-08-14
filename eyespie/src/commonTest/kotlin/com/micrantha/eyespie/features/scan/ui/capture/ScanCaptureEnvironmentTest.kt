package com.micrantha.eyespie.features.scan.ui.capture

import com.micrantha.bluebell.arch.FakeDispatcher
import com.micrantha.eyespie.core.ui.FakeScreenContext
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.features.onboarding.data.CapabilityPermissionGateway
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.onboarding.entities.CapabilityState
import com.micrantha.eyespie.features.onboarding.entities.OnboardingCapability
import com.micrantha.eyespie.features.scan.data.CaptureFileStore
import com.micrantha.eyespie.features.scan.entities.ScanAction.CameraAuthorizationLoaded
import com.micrantha.eyespie.features.scan.entities.ScanAction.CameraAuthorizationRequestFailed
import com.micrantha.eyespie.features.scan.entities.ScanAction.CameraAuthorizationRequestResolved
import com.micrantha.eyespie.features.scan.entities.ScanAction.OpenCameraSettings
import com.micrantha.eyespie.features.scan.entities.ScanAction.RefreshCameraAuthorization
import com.micrantha.eyespie.features.scan.entities.ScanAction.RequestCameraAuthorization
import com.micrantha.eyespie.features.scan.entities.ScanAction.ScanError
import com.micrantha.eyespie.features.scan.entities.ScanState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ScanCaptureEnvironmentTest {
    private class FakeGateway : CapabilityPermissionGateway {
        var loadedAuthorization: CapabilityAuthorization = CapabilityAuthorization.NotRequested
        var requestedAuthorization: CapabilityAuthorization = CapabilityAuthorization.Granted
        var requestFailure: Throwable? = null
        var requestCalls: Int = 0
        var settingsCalls: Int = 0

        override suspend fun loadCapabilities(previous: List<CapabilityState>): List<CapabilityState> =
            listOf(
                CapabilityState(
                    capability = OnboardingCapability.CameraScanning,
                    authorization = loadedAuthorization,
                    canRequestDuringOnboarding = true,
                ),
            )

        override suspend fun requestAuthorization(
            capability: OnboardingCapability,
            previous: CapabilityAuthorization,
        ): CapabilityAuthorization {
            requestCalls += 1
            requestFailure?.let { throw it }
            return requestedAuthorization
        }

        override fun openSettings(capability: OnboardingCapability) {
            settingsCalls += 1
        }
    }

    private class FakeCaptureFileStore : CaptureFileStore {
        val deletedPaths = mutableListOf<Path>()

        override fun delete(path: Path): Result<Unit> = runCatching {
            deletedPaths += path
        }
    }

    private data class Fixture(
        val dispatcher: FakeDispatcher,
        val gateway: FakeGateway,
        val captureFileStore: FakeCaptureFileStore,
        val context: FakeScreenContext,
        val environment: ScanCaptureEnvironment,
    )

    private fun fixture(): Fixture {
        val dispatcher = FakeDispatcher(CoroutineScope(UnconfinedTestDispatcher()))
        val gateway = FakeGateway()
        val captureFileStore = FakeCaptureFileStore()
        val context = FakeScreenContext(dispatcher = dispatcher)
        return Fixture(
            dispatcher = dispatcher,
            gateway = gateway,
            captureFileStore = captureFileStore,
            context = context,
            environment = ScanCaptureEnvironment(context, gateway, captureFileStore),
        )
    }

    @Test
    fun `navigation failure deletes capture before restoring scan state`() = runTest {
        val fixture = fixture()
        val capturePath = "/capture/eyespie-capture-navigation-failure.jpg".toPath()
        fixture.context.router.navigateFailure = IllegalStateException("navigation failed")

        fixture.environment.invoke(
            capturePath,
            ScanState(location = Location()),
        )

        assertEquals(listOf(capturePath), fixture.captureFileStore.deletedPaths)
        assertTrue(fixture.dispatcher.actions.any { it is ScanError })
    }

    @Test
    fun `successful edit handoff retains capture ownership`() = runTest {
        val fixture = fixture()
        val capturePath = "/capture/eyespie-capture-edit.jpg".toPath()

        fixture.environment.invoke(
            capturePath,
            ScanState(location = Location()),
        )

        assertTrue(fixture.captureFileStore.deletedPaths.isEmpty())
    }

    @Test
    fun `refresh dispatches OS authoritative camera state with its input snapshot`() = runTest {
        val fixture = fixture()
        fixture.gateway.loadedAuthorization = CapabilityAuthorization.Granted
        val state = ScanState(
            cameraAuthorizationLoaded = true,
            cameraAuthorization = CapabilityAuthorization.Denied,
        )

        fixture.environment.invoke(RefreshCameraAuthorization, state)

        val action = fixture.dispatcher.actions.filterIsInstance<CameraAuthorizationLoaded>().last()
        assertEquals(CapabilityAuthorization.Denied, action.previous)
        assertEquals(CapabilityAuthorization.Granted, action.authorization)
    }

    @Test
    fun `refresh is ignored while camera request is active`() = runTest {
        val fixture = fixture()
        val request = RequestCameraAuthorization()
        fixture.environment.invoke(
            RefreshCameraAuthorization,
            ScanState(
                cameraAuthorizationLoaded = true,
                cameraRequestId = request.requestId,
            ),
        )

        assertTrue(fixture.dispatcher.actions.none { it is CameraAuthorizationLoaded })
    }

    @Test
    fun `immediate request result resolves the accepted request identity`() = runTest {
        val fixture = fixture()
        fixture.gateway.requestedAuthorization = CapabilityAuthorization.Denied
        val initial = ScanState(
            cameraAuthorizationLoaded = true,
            cameraAuthorization = CapabilityAuthorization.NotRequested,
        )
        val request = RequestCameraAuthorization()
        val accepted = fixture.environment.reduce(initial, request)

        assertTrue(accepted.cameraRequestInFlight)
        assertTrue(accepted.cameraRequestId === request.requestId)

        fixture.environment.invoke(request, accepted)

        assertEquals(1, fixture.gateway.requestCalls)
        val resolvedAction = fixture.dispatcher.actions
            .filterIsInstance<CameraAuthorizationRequestResolved>()
            .last()
        assertTrue(resolvedAction.requestId === request.requestId)
        assertEquals(CapabilityAuthorization.Denied, resolvedAction.authorization)

        val resolved = fixture.environment.reduce(accepted, resolvedAction)
        assertFalse(resolved.cameraRequestInFlight)
        assertEquals(CapabilityAuthorization.Denied, resolved.cameraAuthorization)
    }

    @Test
    fun `unaccepted request intent cannot call native authorization`() = runTest {
        val fixture = fixture()
        val unloadedRequest = RequestCameraAuthorization()
        val unloaded = fixture.environment.reduce(ScanState(), unloadedRequest)
        fixture.environment.invoke(unloadedRequest, unloaded)

        val settingsRequest = RequestCameraAuthorization()
        val settingsRequired = ScanState(
            cameraAuthorizationLoaded = true,
            cameraAuthorization = CapabilityAuthorization.SettingsRequired,
        )
        val rejected = fixture.environment.reduce(settingsRequired, settingsRequest)
        fixture.environment.invoke(settingsRequest, rejected)

        assertEquals(0, fixture.gateway.requestCalls)
        assertFalse(unloaded.cameraRequestInFlight)
        assertFalse(rejected.cameraRequestInFlight)
    }

    @Test
    fun `duplicate request identity is rejected before its effect runs`() = runTest {
        val fixture = fixture()
        val initial = ScanState(
            cameraAuthorizationLoaded = true,
            cameraAuthorization = CapabilityAuthorization.NotRequested,
        )
        val first = RequestCameraAuthorization()
        val second = RequestCameraAuthorization()
        val accepted = fixture.environment.reduce(initial, first)
        val duplicateRejected = fixture.environment.reduce(accepted, second)

        assertTrue(duplicateRejected.cameraRequestId === first.requestId)

        fixture.environment.invoke(second, duplicateRejected)
        assertEquals(0, fixture.gateway.requestCalls)

        fixture.environment.invoke(first, duplicateRejected)
        assertEquals(1, fixture.gateway.requestCalls)
    }

    @Test
    fun `cancellation clears matching request and propagates`() = runTest {
        val fixture = fixture()
        fixture.gateway.requestFailure = CancellationException("cancelled")
        val initial = ScanState(
            cameraAuthorizationLoaded = true,
            cameraAuthorization = CapabilityAuthorization.NotRequested,
        )
        val request = RequestCameraAuthorization()
        val accepted = fixture.environment.reduce(initial, request)

        assertFailsWith<CancellationException> {
            fixture.environment.invoke(request, accepted)
        }

        val failedAction = fixture.dispatcher.actions
            .filterIsInstance<CameraAuthorizationRequestFailed>()
            .last()
        assertTrue(failedAction.requestId === request.requestId)
        assertFalse(fixture.environment.reduce(accepted, failedAction).cameraRequestInFlight)
    }

    @Test
    fun `settings open only for settings required state`() = runTest {
        val fixture = fixture()

        fixture.environment.invoke(
            OpenCameraSettings,
            ScanState(
                cameraAuthorizationLoaded = true,
                cameraAuthorization = CapabilityAuthorization.Denied,
            ),
        )
        fixture.environment.invoke(
            OpenCameraSettings,
            ScanState(
                cameraAuthorizationLoaded = true,
                cameraAuthorization = CapabilityAuthorization.SettingsRequired,
            ),
        )

        assertEquals(1, fixture.gateway.settingsCalls)
    }

    @Test
    fun `fresh authorization refresh is applied`() {
        val fixture = fixture()
        val state = ScanState(
            cameraAuthorizationLoaded = true,
            cameraAuthorization = CapabilityAuthorization.NotRequested,
        )

        val result = fixture.environment.reduce(
            state,
            CameraAuthorizationLoaded(
                previous = CapabilityAuthorization.NotRequested,
                authorization = CapabilityAuthorization.Granted,
            ),
        )

        assertEquals(CapabilityAuthorization.Granted, result.cameraAuthorization)
        assertTrue(result.cameraAuthorizationLoaded)
    }

    @Test
    fun `stale refresh cannot overwrite a denied request result`() {
        val fixture = fixture()
        val resolved = ScanState(
            cameraAuthorizationLoaded = true,
            cameraAuthorization = CapabilityAuthorization.Denied,
        )

        val result = fixture.environment.reduce(
            resolved,
            CameraAuthorizationLoaded(
                previous = CapabilityAuthorization.NotRequested,
                authorization = CapabilityAuthorization.NotRequested,
            ),
        )

        assertEquals(resolved, result)
    }

    @Test
    fun `stale result cannot resolve a newer request`() {
        val fixture = fixture()
        val initial = ScanState(
            cameraAuthorizationLoaded = true,
            cameraAuthorization = CapabilityAuthorization.Denied,
        )
        val first = RequestCameraAuthorization()
        val firstAccepted = fixture.environment.reduce(initial, first)
        val firstFailed = fixture.environment.reduce(
            firstAccepted,
            CameraAuthorizationRequestFailed(first.requestId),
        )
        val second = RequestCameraAuthorization()
        val secondAccepted = fixture.environment.reduce(firstFailed, second)

        val staleResult = fixture.environment.reduce(
            secondAccepted,
            CameraAuthorizationRequestResolved(
                requestId = first.requestId,
                authorization = CapabilityAuthorization.Granted,
            ),
        )

        assertEquals(secondAccepted, staleResult)
        assertTrue(staleResult.cameraRequestId === second.requestId)
    }
}
