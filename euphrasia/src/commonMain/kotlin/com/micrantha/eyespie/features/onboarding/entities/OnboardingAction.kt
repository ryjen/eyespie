package com.micrantha.eyespie.features.onboarding.entities

sealed interface OnboardingAction {
    data object Init : OnboardingAction
    data object SkipDownload: OnboardingAction
    data class Loaded(val models: OnboardingModels) : OnboardingAction
    data object NextPage : OnboardingAction
    data object Done : OnboardingAction
    data object Download : OnboardingAction
    data class SelectModel(var name: String) : OnboardingAction
    data class PageChanged(val page: Int) : OnboardingAction
    data class Error(val error: Throwable) : OnboardingAction
}
