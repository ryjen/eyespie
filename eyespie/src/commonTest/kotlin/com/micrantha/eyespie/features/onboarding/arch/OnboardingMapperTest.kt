package com.micrantha.eyespie.features.onboarding.arch

import com.micrantha.eyespie.features.onboarding.entities.CapabilityAction
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.onboarding.entities.CapabilityState
import com.micrantha.eyespie.features.onboarding.entities.OnboardingCapability
import com.micrantha.eyespie.features.onboarding.entities.OnboardingState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OnboardingMapperTest {
    @Test
    fun `not requested camera maps to allow action`() {
        val capability = mapCamera(CapabilityAuthorization.NotRequested)

        assertEquals(CapabilityAction.Request, capability.action)
        assertEquals("Allow", capability.actionLabel)
    }

    @Test
    fun `retryable camera denial maps to try again action`() {
        val capability = mapCamera(CapabilityAuthorization.Denied)

        assertEquals(CapabilityAction.Request, capability.action)
        assertEquals("Try again", capability.actionLabel)
    }

    @Test
    fun `settings-required camera maps to open settings action`() {
        val capability = mapCamera(CapabilityAuthorization.SettingsRequired)

        assertEquals(CapabilityAction.OpenSettings, capability.action)
        assertEquals("Open settings", capability.actionLabel)
    }

    @Test
    fun `granted and restricted cameras are not actionable`() {
        assertNull(mapCamera(CapabilityAuthorization.Granted).action)
        assertNull(mapCamera(CapabilityAuthorization.Restricted).action)
    }

    @Test
    fun `notification prompt remains deferred during onboarding`() {
        val state = OnboardingState(
            capabilities = listOf(
                CapabilityState(
                    capability = OnboardingCapability.Notifications,
                    authorization = CapabilityAuthorization.NotRequested,
                    canRequestDuringOnboarding = false,
                )
            )
        )

        val capability = OnboardingMapper.map(state).capabilities.single()

        assertNull(capability.action)
        assertNull(capability.actionLabel)
    }

    @Test
    fun `unsupported and permission-free capabilities are not rendered`() {
        val state = OnboardingState(
            capabilities = listOf(
                camera(CapabilityAuthorization.Unsupported),
                CapabilityState(
                    capability = OnboardingCapability.Notifications,
                    authorization = CapabilityAuthorization.NotRequired,
                    canRequestDuringOnboarding = false,
                ),
            )
        )

        assertEquals(emptyList(), OnboardingMapper.map(state).capabilities)
    }

    private fun mapCamera(authorization: CapabilityAuthorization) = OnboardingMapper.map(
        OnboardingState(capabilities = listOf(camera(authorization)))
    ).capabilities.single()

    private fun camera(authorization: CapabilityAuthorization) = CapabilityState(
        capability = OnboardingCapability.CameraScanning,
        authorization = authorization,
        canRequestDuringOnboarding = true,
    )
}
