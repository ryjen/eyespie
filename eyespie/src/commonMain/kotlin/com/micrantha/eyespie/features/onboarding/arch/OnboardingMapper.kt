package com.micrantha.eyespie.features.onboarding.arch

import com.micrantha.bluebell.arch.StateMapper
import com.micrantha.bluebell.i18n.repository.LocalizedRepository
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.onboarding.entities.CapabilityUiState
import com.micrantha.eyespie.features.onboarding.entities.OnboardingCapability
import com.micrantha.eyespie.features.onboarding.entities.OnboardingState
import com.micrantha.eyespie.features.onboarding.entities.OnboardingUiState

class OnboardingMapper(
    private val context: ScreenContext,
) : LocalizedRepository by context.i18n {

    fun map(state: OnboardingState) = OnboardingMapper.map(state)

    companion object : StateMapper<OnboardingState, OnboardingUiState> {
        override fun map(state: OnboardingState) = OnboardingUiState(
            isBusy = state.isInitializing,
            isError = state.error != null,
            page = state.page,
            isSelected = state.selectedModel?.let { state.models?.containsKey(it) } ?: false,
            models = state.models?.map {
                OnboardingUiState.Model(
                    it.key, it.key == state.selectedModel
                )
            } ?: emptyList(),
            capabilities = listOf(
                CapabilityUiState(
                    capability = OnboardingCapability.CameraScanning,
                    title = "Scan with the camera",
                    rationale = "Eyespie uses the camera to scan real-world scenes and objects for gameplay.",
                    deniedImpact = "You can continue setup, but scanning will remain unavailable until camera access is enabled.",
                    privacySummary = "Camera frames are used for the active scan. They are not permission telemetry.",
                    authorization = CapabilityAuthorization.NotRequested,
                    canRequestDuringOnboarding = true,
                ),
                CapabilityUiState(
                    capability = OnboardingCapability.Notifications,
                    title = "Game and download notifications",
                    rationale = "Notifications can report game events and long-running model download progress.",
                    deniedImpact = "Status remains available in the app, but background alerts will not be shown.",
                    privacySummary = "Notification permission does not grant access to contacts, media, or scanned content.",
                    authorization = CapabilityAuthorization.NotRequested,
                    canRequestDuringOnboarding = false,
                ),
            ),
        )
    }
}
