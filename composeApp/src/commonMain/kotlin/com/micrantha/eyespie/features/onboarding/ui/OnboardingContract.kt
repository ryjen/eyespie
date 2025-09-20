package com.micrantha.eyespie.features.onboarding.ui

import com.micrantha.eyespie.domain.entities.ModelFile

enum class OnboardingPage {
    Welcome,
    GenAI,
}

data class OnboardingState(
    val page: OnboardingPage = OnboardingPage.Welcome,
    val isDownloading: Boolean = true,
    val error: Throwable? = null,
    val models: List<ModelFile> = emptyList(),
    val selectedModel: ModelFile? = null
)

data class OnboardingUiState(
    val page: OnboardingPage,
    val isBusy: Boolean,
    val isError: Boolean,
    val models: List<ModelFile>,
    val selectedModel: ModelFile?
)

sealed interface OnboardingAction {
    data object Init : OnboardingAction
    data object NextPage : OnboardingAction
    data object Done : OnboardingAction
    data class PageChanged(val page: Int) : OnboardingAction
    data object Download : OnboardingAction
    data class Error(val error: Throwable) : OnboardingAction
    data class LoadedModels(val models: List<ModelFile>) : OnboardingAction
    data class SelectedModel(val model: ModelFile) : OnboardingAction
}
