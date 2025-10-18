package com.micrantha.eyespie.features.onboarding.domain.entities

import com.micrantha.eyespie.domain.entities.UrlFile

sealed interface OnboardingAction {
    data object Init : OnboardingAction
    data class Loaded(val models: Map<String, UrlFile>) : OnboardingAction
    data object NextPage : OnboardingAction
    data object Done : OnboardingAction
    data class SelectModel(var name: String) : OnboardingAction
    data class PageChanged(val page: Int) : OnboardingAction
    data object StartGenAI : OnboardingAction
    data class Error(val error: Throwable) : OnboardingAction
}
