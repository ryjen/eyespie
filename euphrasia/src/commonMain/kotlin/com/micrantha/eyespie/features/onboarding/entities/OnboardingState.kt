package com.micrantha.eyespie.features.onboarding.entities

import com.micrantha.bluebell.plugin.BluebellAssetConfig

typealias OnboardingModels = Map<String, BluebellAssetConfig.Download>

data class OnboardingState(
    val page: OnboardingPage = OnboardingPage.Welcome,
    val error: Throwable? = null,
    val isInitializing: Boolean = true,
    val models: OnboardingModels? = null,
    val selectedModel: String? = null,
)
