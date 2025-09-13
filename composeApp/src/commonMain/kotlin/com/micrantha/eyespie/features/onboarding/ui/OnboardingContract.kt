package com.micrantha.eyespie.features.onboarding.ui

enum class OnboardingPage {
    Welcome,
    GenAI,
}

data class OnboardingState(
    val page: OnboardingPage = OnboardingPage.Welcome,
    val isDownloading: Boolean = false,
    val error: Throwable? = null
)

data class OnboardingUiState(
    val page: OnboardingPage,
    val isBusy: Boolean,
    val isError: Boolean,
)

sealed interface OnboardingAction {
    data object NextPage : OnboardingAction
    data object Done : OnboardingAction
    data class PageChanged(val page: Int) : OnboardingAction
    data object Download : OnboardingAction
    data class DownloadError(val error: Throwable) : OnboardingAction
}
