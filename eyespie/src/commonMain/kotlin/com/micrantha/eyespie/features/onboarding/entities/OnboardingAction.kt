package com.micrantha.eyespie.features.onboarding.entities

sealed interface OnboardingAction {
    data object Init : OnboardingAction
    data object RefreshCapabilities : OnboardingAction
    data object SkipDownload : OnboardingAction
    data class Loaded(val models: OnboardingModels) : OnboardingAction
    data class CapabilitiesLoaded(val capabilities: List<CapabilityState>) : OnboardingAction
    data object NextPage : OnboardingAction
    data object Done : OnboardingAction
    data object Download : OnboardingAction
    data class SelectModel(var name: String) : OnboardingAction
    data class PageChanged(val page: Int) : OnboardingAction
    data class RequestCapability(val capability: OnboardingCapability) : OnboardingAction
    data class CapabilityRequestStarted(val capability: OnboardingCapability) : OnboardingAction
    data class CapabilityRequestResolved(
        val capability: OnboardingCapability,
        val authorization: CapabilityAuthorization,
    ) : OnboardingAction

    data class CapabilityRequestFailed(val capability: OnboardingCapability) : OnboardingAction
    data class OpenCapabilitySettings(val capability: OnboardingCapability) : OnboardingAction
    data class Error(val error: Throwable) : OnboardingAction
}
