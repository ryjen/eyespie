package com.micrantha.eyespie.features.onboarding.entities

data class OnboardingUiState(
    val page: OnboardingPage,
    val isBusy: Boolean,
    val isError: Boolean,
    val isSelected: Boolean,
    val models: List<Model>,
    val capabilities: List<CapabilityUiState> = emptyList(),
    val requestInFlight: OnboardingCapability? = null,
) {
    data class Model(
        val name: String,
        val isSelected: Boolean
    )
}
