package com.micrantha.eyespie.features.onboarding.arch

import com.micrantha.bluebell.arch.StateMapper
import com.micrantha.bluebell.i18n.repository.LocalizedRepository
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAction
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.onboarding.entities.CapabilityState
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
                    it.key,
                    it.key == state.selectedModel,
                )
            } ?: emptyList(),
            capabilities = state.capabilities.map(::mapCapability),
            requestInFlight = state.requestInFlight,
        )

        private fun mapCapability(state: CapabilityState): CapabilityUiState {
            val (action, actionLabel) = state.action()
            return when (state.capability) {
                OnboardingCapability.CameraScanning -> CapabilityUiState(
                    capability = state.capability,
                    title = "Scan with the camera",
                    rationale = "Eyespie uses the camera when you choose to scan real-world scenes and objects for gameplay.",
                    deniedImpact = "You can continue setup, but scanning remains unavailable until camera access is enabled.",
                    privacySummary = "Camera access does not grant photo-library access. Captured data is handled by the active scan flow.",
                    authorization = state.authorization,
                    action = action,
                    actionLabel = actionLabel,
                )

                OnboardingCapability.Notifications -> CapabilityUiState(
                    capability = state.capability,
                    title = "Game and download notifications",
                    rationale = "Notifications can report game events and long-running model download progress.",
                    deniedImpact = "Status remains available in the app, but background alerts will not be shown.",
                    privacySummary = "Notification access does not grant access to contacts, media, scans, or generated content.",
                    authorization = state.authorization,
                    action = action,
                    actionLabel = actionLabel,
                )
            }
        }

        private fun CapabilityState.action(): Pair<CapabilityAction?, String?> {
            if (!canRequestDuringOnboarding) return null to null
            return when (authorization) {
                CapabilityAuthorization.NotRequested -> CapabilityAction.Request to "Allow"
                CapabilityAuthorization.Denied -> CapabilityAction.Request to "Try again"
                CapabilityAuthorization.SettingsRequired -> CapabilityAction.OpenSettings to "Open settings"
                else -> null to null
            }
        }
    }
}
