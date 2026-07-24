package com.micrantha.eyespie.features.onboarding.entities

enum class OnboardingCapability {
    CameraScanning,
    Notifications,
}

sealed interface CapabilityAuthorization {
    data object Unsupported : CapabilityAuthorization
    data object NotRequired : CapabilityAuthorization
    data object NotRequested : CapabilityAuthorization
    data object Granted : CapabilityAuthorization
    data object Denied : CapabilityAuthorization
    data object Restricted : CapabilityAuthorization
    data object SettingsRequired : CapabilityAuthorization
}

data class CapabilityState(
    val capability: OnboardingCapability,
    val authorization: CapabilityAuthorization = CapabilityAuthorization.NotRequested,
    val canRequestDuringOnboarding: Boolean,
)

data class CapabilityUiState(
    val capability: OnboardingCapability,
    val title: String,
    val rationale: String,
    val deniedImpact: String,
    val privacySummary: String,
    val authorization: CapabilityAuthorization,
    val canRequestDuringOnboarding: Boolean,
)
