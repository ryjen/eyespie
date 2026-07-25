package com.micrantha.eyespie.features.onboarding.data

import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import dev.icerock.moko.permissions.PermissionState
import kotlin.test.Test
import kotlin.test.assertEquals

class CapabilityPermissionGatewayTest {
    @Test
    fun `permission states normalize to capability authorization`() {
        assertEquals(
            CapabilityAuthorization.NotRequested,
            PermissionState.NotDetermined.toCapabilityAuthorization(),
        )
        assertEquals(
            CapabilityAuthorization.NotRequested,
            PermissionState.NotGranted.toCapabilityAuthorization(),
        )
        assertEquals(
            CapabilityAuthorization.Granted,
            PermissionState.Granted.toCapabilityAuthorization(),
        )
        assertEquals(
            CapabilityAuthorization.Denied,
            PermissionState.Denied.toCapabilityAuthorization(),
        )
        assertEquals(
            CapabilityAuthorization.SettingsRequired,
            PermissionState.DeniedAlways.toCapabilityAuthorization(),
        )
    }

    @Test
    fun `ambiguous Android not-granted state preserves known denial`() {
        assertEquals(
            CapabilityAuthorization.Denied,
            PermissionState.NotGranted.toCapabilityAuthorization(CapabilityAuthorization.Denied),
        )
        assertEquals(
            CapabilityAuthorization.SettingsRequired,
            PermissionState.NotGranted.toCapabilityAuthorization(
                CapabilityAuthorization.SettingsRequired
            ),
        )
    }
}
