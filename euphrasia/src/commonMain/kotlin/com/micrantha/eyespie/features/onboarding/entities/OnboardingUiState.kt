package com.micrantha.eyespie.features.onboarding.entities

data class OnboardingUiState(
    val page: OnboardingPage,
    val isBusy: Boolean,
    val isError: Boolean,
    val isSelected: Boolean,
    val models: List<Model>,
) {
    data class Model(
        val name: String,
        val isSelected: Boolean
    )
}
