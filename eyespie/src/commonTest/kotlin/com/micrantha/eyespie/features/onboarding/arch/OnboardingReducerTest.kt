package com.micrantha.eyespie.features.onboarding.arch

import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.onboarding.entities.CapabilityState
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.CapabilityRequestFailed
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.CapabilityRequestResolved
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.CapabilityRequestStarted
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.PageChanged
import com.micrantha.eyespie.features.onboarding.entities.OnboardingCapability
import com.micrantha.eyespie.features.onboarding.entities.OnboardingPage
import com.micrantha.eyespie.features.onboarding.entities.OnboardingState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OnboardingReducerTest {
    private val reducer = OnboardingReducer()
    private val camera = CapabilityState(
        capability = OnboardingCapability.CameraScanning,
        canRequestDuringOnboarding = true,
    )
    private val notifications = CapabilityState(
        capability = OnboardingCapability.Notifications,
        canRequestDuringOnboarding = false,
    )

    @Test
    fun `page change selects permissions page`() {
        val result = reducer.reduce(
            OnboardingState(),
            PageChanged(OnboardingPage.Permissions.ordinal),
        )

        assertEquals(OnboardingPage.Permissions, result.page)
    }

    @Test
    fun `invalid page change is ignored`() {
        val state = OnboardingState()

        assertEquals(state, reducer.reduce(state, PageChanged(Int.MAX_VALUE)))
    }

    @Test
    fun `camera request starts when no request is active`() {
        val result = reducer.reduce(
            OnboardingState(capabilities = listOf(camera, notifications)),
            CapabilityRequestStarted(OnboardingCapability.CameraScanning),
        )

        assertEquals(OnboardingCapability.CameraScanning, result.requestInFlight)
    }

    @Test
    fun `deferred notification request cannot start during onboarding`() {
        val state = OnboardingState(capabilities = listOf(camera, notifications))

        assertEquals(
            state,
            reducer.reduce(
                state,
                CapabilityRequestStarted(OnboardingCapability.Notifications),
            ),
        )
    }

    @Test
    fun `second request cannot replace active request`() {
        val state = OnboardingState(
            capabilities = listOf(camera, notifications),
            requestInFlight = OnboardingCapability.CameraScanning,
        )

        assertEquals(
            state,
            reducer.reduce(
                state,
                CapabilityRequestStarted(OnboardingCapability.Notifications),
            ),
        )
    }

    @Test
    fun `resolved request updates only selected capability and clears request`() {
        val state = OnboardingState(
            capabilities = listOf(camera, notifications),
            requestInFlight = OnboardingCapability.CameraScanning,
        )

        val result = reducer.reduce(
            state,
            CapabilityRequestResolved(
                OnboardingCapability.CameraScanning,
                CapabilityAuthorization.Granted,
            ),
        )

        assertEquals(CapabilityAuthorization.Granted, result.capabilities.first().authorization)
        assertEquals(notifications, result.capabilities.last())
        assertNull(result.requestInFlight)
    }

    @Test
    fun `failed request clears matching in-flight request without changing authorization`() {
        val state = OnboardingState(
            capabilities = listOf(camera),
            requestInFlight = OnboardingCapability.CameraScanning,
        )

        val result = reducer.reduce(
            state,
            CapabilityRequestFailed(OnboardingCapability.CameraScanning),
        )

        assertEquals(camera, result.capabilities.single())
        assertNull(result.requestInFlight)
    }
}
