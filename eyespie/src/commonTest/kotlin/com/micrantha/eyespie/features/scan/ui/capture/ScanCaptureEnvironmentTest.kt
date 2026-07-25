package com.micrantha.eyespie.features.scan.ui.capture

import com.micrantha.bluebell.arch.FakeDispatcher
import com.micrantha.eyespie.core.ui.FakeScreenContext
import com.micrantha.eyespie.features.onboarding.data.CapabilityPermissionGateway
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.onboarding.entities.CapabilityState
import com.micrantha.eyespie.features.onboarding.entities.OnboardingCapability
import com.micrantha.eyespie.features.scan.entities.ScanAction.CameraAuthorizationLoaded
import com.micrantha.eyespie.features.scan.entities.ScanAction.CameraAuthorizationRequestFailed
import com.micrantha.eyespie.features.scan.entities.ScanAction.CameraAuthorizationRequestResolved
import com.micrantha.eyespie.features.scan.entities.ScanAction.CameraAuthorizationRequestStarted
import com.micrantha.eyespie.features.scan.entities.ScanAction.OpenCameraSettings
import com.micrantha.eyespie.features.scan.entities.ScanAction.RefreshCameraAuthorization
import com.micrantha.eyespie.features.scan.entities.ScanAction.RequestCameraAuthorization
import com.micrantha.eyespie.features.scan.entities.ScanState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

    private data class Fixture(
        val dispatcher: FakeDispatcher,
        val gateway: FakeGateway,
        val environment: ScanCaptureEnvironment,
    )

    private fun fixture(): Fixture {
        val dispatcher = FakeDispatcher(CoroutineScope(UnconfinedTestDispatcher()))
        val gateway = FakeGateway()
        val context = FakeScreenContext(dispatcher = dispatcher)
        return Fixture(
            dispatcher = dispatcher,
            gateway = gateway,
            environment = ScanCaptureEnvironment(context, gateway),
        )
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
        fixture.environment.invoke(
            RefreshCameraAuthorization,
            ScanState(
                cameraAuthorizationLoaded = true,
                cameraRequestInFlight = true,
            ),
        )

        assertTrue(fixture.dispatcher.actions.none { it is CameraAuthorizationLoaded })
    }

    @Test
    fun `explicit request dispatches started and resolved actions`() = runTest {
        val fixture = fixture()
        fixture.gateway.requestedAuthorization = CapabilityAuthorization.Denied
        val state = ScanState(
            cameraAuthorizationLoaded = true,
            cameraAuthorization = CapabilityAuthorization.NotRequested,
        )

        fixture.environment.invoke(RequestCameraAuthorization, state)

        assertEquals(1, fixture.gateway.requestCalls)
        assertTrue(fixture.dispatcher.actions.any { it is CameraAuthorizationRequestStarted })
        assertEquals(
            CapabilityAuthorization.Denied,
            fixture.dispatcher.actions
                .filterIsInstance<CameraAuthorizationRequestResolved>()
                .last()
                .authorization,
        )
    }

    @Test
    fun `request is ignored until authorization is loaded and when settings are required`() = runTest {
        val fixture = fixture()

        fixture.environment.invoke(RequestCameraAuthorization, ScanState())
        fixture.environment.invoke(
            RequestCameraAuthorization,
            ScanState(
                cameraAuthorizationLoaded = true,
                cameraAuthorization = CapabilityAuthorization.SettingsRequired,
            ),
        )

        assertEquals(0, fixture.gateway.requestCalls)
    }

    @Test
    fun `cancellation clears request and propagates`() = runTest {
        val fixture = fixture()
        fixture.gateway.requestFailure = CancellationException("cancelled")
        val state = ScanState(
            cameraAuthorizationLoaded = true,
            cameraAuthorization = CapabilityAuthorization.NotRequested,
        )

        assertFailsWith<CancellationException> {
            fixture.environment.invoke(RequestCameraAuthorization, state)
        }
        assertTrue(fixture.dispatcher.actions.any { it is CameraAuthorizationRequestFailed })
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
    fun `request lifecycle updates only requestable loaded state`() {
        val fixture = fixture()
        val initial = ScanState(
            cameraAuthorizationLoaded = true,
            cameraAuthorization = CapabilityAuthorization.NotRequested,
        )

        val requesting = fixture.environment.reduce(initial, CameraAuthorizationRequestStarted)
        val resolved = fixture.environment.reduce(
            requesting,
            CameraAuthorizationRequestResolved(CapabilityAuthorization.Granted),
        )

        assertTrue(requesting.cameraRequestInFlight)
        assertFalse(resolved.cameraRequestInFlight)
        assertEquals(CapabilityAuthorization.Granted, resolved.cameraAuthorization)
    }
}

class ScanCameraRequestCoordinatorTest {
    @Test
    fun `concurrent request is rejected instead of queued`() = runTest {
        val coordinator = ScanCameraRequestCoordinator()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        val first = async {
            coordinator.runExclusive {
                entered.complete(Unit)
                release.await()
                "first"
            }
        }
        entered.await()

        val second = coordinator.runExclusive { "second" }
        assertNull(second)

        release.complete(Unit)
        assertEquals("first", first.await())
    }
}
