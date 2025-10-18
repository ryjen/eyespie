package com.micrantha.eyespie.features.onboarding.domain.entities

data class OnboardingUiState(
    val page: OnboardingPage,
    val isBusy: Boolean,
    val isError: Boolean,
    val models: List<Model>,
) {
    data class Model(
        val name: String,
        val isSelected: Boolean
    )
}
