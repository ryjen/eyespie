package com.micrantha.eyespie.features.onboarding.domain.entities

import com.micrantha.eyespie.domain.entities.UrlFile

data class OnboardingState(
    val page: OnboardingPage = OnboardingPage.Welcome,
    val error: Throwable? = null,
    val isInitializing: Boolean = false,
    val models: Map<String, UrlFile>? = null,
    val selectedModel: String? = null,
)
