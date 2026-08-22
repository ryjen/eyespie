package com.micrantha.eyespie.features.onboarding

enum class OnboardingPage {
    Local,
    Create,
    Share,
    Join,
}

data class OnboardingState(
    val page: OnboardingPage = OnboardingPage.Local,
)
