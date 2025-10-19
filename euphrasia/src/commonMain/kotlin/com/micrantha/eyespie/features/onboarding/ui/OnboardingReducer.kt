package com.micrantha.bluebell.com.micrantha.eyespie.features.onboarding.ui

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingAction
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingPage
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingState

class OnboardingReducer : Reducer<OnboardingState> {
    override fun reduce(
        state: OnboardingState,
        action: Action
    ): OnboardingState = when (action) {
        is OnboardingAction.Init -> state.copy(
            isInitializing = true,
            error = null
        )

        is OnboardingAction.Error -> state.copy(
            error = action.error,
            isInitializing = false
        )

        is OnboardingAction.SelectModel -> state.copy(
            selectedModel = action.name
        )

        is OnboardingAction.PageChanged -> state.copy(
            page = OnboardingPage.entries[action.page]
        )

        is OnboardingAction.Done -> state.copy(
            isInitializing = false
        )

        is OnboardingAction.Loaded -> state.copy(
            models = action.models,
            isInitializing = false,
            error = null
        )

        else -> state
    }
}
