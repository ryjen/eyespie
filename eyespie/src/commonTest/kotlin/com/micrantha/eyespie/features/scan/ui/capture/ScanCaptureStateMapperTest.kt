package com.micrantha.eyespie.features.scan.ui.capture

import com.micrantha.eyespie.features.onboarding.entities.CapabilityAction
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.scan.entities.ScanState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScanCaptureStateMapperTest {
    private val mapper = ScanCaptureStateMapper()

    @Test
    fun `authorization action remains unavailable until OS state is loaded`() {
        val result = mapper.map(ScanState())

        assertFalse(result.cameraAuthorizationLoaded)
        assertFalse(result.cameraReady)
        assertNull(result.cameraAction)
    }

    @Test
    fun `not requested camera shows explicit allow action`() {
        val result = mapper.map(
            ScanState(
                cameraAuthorizationLoaded = true,
                cameraAuthorization = CapabilityAuthorization.NotRequested,
            ),
        )

        assertEquals(CapabilityAction.Request, result.cameraAction)
        assertEquals("Allow camera", result.cameraActionLabel)
        assertFalse(result.cameraReady)
    }

    @Test
    fun `retryable denial shows try again action`() {
        val result = mapper.map(
            ScanState(
                cameraAuthorizationLoaded = true,
                cameraAuthorization = CapabilityAuthorization.Denied,
            ),
        )

        assertEquals(CapabilityAction.Request, result.cameraAction)
        assertEquals("Try again", result.cameraActionLabel)
    }

    @Test
    fun `settings required denial shows settings action`() {
        val result = mapper.map(
            ScanState(
                cameraAuthorizationLoaded = true,
                cameraAuthorization = CapabilityAuthorization.SettingsRequired,
            ),
        )

        assertEquals(CapabilityAction.OpenSettings, result.cameraAction)
        assertEquals("Open settings", result.cameraActionLabel)
    }

    @Test
    fun `granted and permission free camera states are ready`() {
        assertTrue(
            mapper.map(
                ScanState(
                    cameraAuthorizationLoaded = true,
                    cameraAuthorization = CapabilityAuthorization.Granted,
                ),
            ).cameraReady,
        )
        assertTrue(
            mapper.map(
                ScanState(
                    cameraAuthorizationLoaded = true,
                    cameraAuthorization = CapabilityAuthorization.NotRequired,
                ),
            ).cameraReady,
        )
    }

    @Test
    fun `restricted and unsupported states do not offer recovery actions`() {
        val restricted = mapper.map(
            ScanState(
                cameraAuthorizationLoaded = true,
                cameraAuthorization = CapabilityAuthorization.Restricted,
            ),
        )
        val unsupported = mapper.map(
            ScanState(
                cameraAuthorizationLoaded = true,
                cameraAuthorization = CapabilityAuthorization.Unsupported,
            ),
        )

        assertNull(restricted.cameraAction)
        assertFalse(restricted.cameraReady)
        assertNull(unsupported.cameraAction)
        assertFalse(unsupported.cameraReady)
    }
}
