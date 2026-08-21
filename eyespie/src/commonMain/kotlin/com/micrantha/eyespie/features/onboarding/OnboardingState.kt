package com.micrantha.eyespie.features.onboarding

enum class OnboardingPage {
    Welcome,
    Create,
    Play,
}

data class OnboardingState(
    val page: OnboardingPage = OnboardingPage.Welcome,
)
