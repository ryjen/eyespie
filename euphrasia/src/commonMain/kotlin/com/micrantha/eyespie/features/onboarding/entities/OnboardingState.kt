package com.micrantha.eyespie.features.onboarding.entities


typealias OnboardingModels = Map<String, AiModel>

data class OnboardingState(
    val page: OnboardingPage = OnboardingPage.Welcome,
    val error: Throwable? = null,
    val isInitializing: Boolean = true,
    val models: OnboardingModels? = null,
    val selectedModel: String? = null,
)
