package com.micrantha.eyespie.features.onboarding.ui

enum class OnboardingPage {
    Welcome,
    GenAI,
}

data class OnboardingState(
    val page: OnboardingPage = OnboardingPage.Welcome,
    val error: Throwable? = null,
    val isInitializing: Boolean = false,
)

data class OnboardingUiState(
    val page: OnboardingPage,
    val isBusy: Boolean,
    val isError: Boolean,
)

sealed interface OnboardingAction {
    data object Init : OnboardingAction
    data object NextPage : OnboardingAction
    data object Done : OnboardingAction
    data class PageChanged(val page: Int) : OnboardingAction
    data object StartGenAI : OnboardingAction
    data class Error(val error: Throwable) : OnboardingAction
}
