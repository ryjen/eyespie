package com.micrantha.eyespie.features.onboarding.arch

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.*
import com.micrantha.eyespie.features.onboarding.entities.OnboardingPage
import com.micrantha.eyespie.features.onboarding.entities.OnboardingState

class OnboardingReducer : Reducer<OnboardingState> {
    override fun reduce(
        state: OnboardingState,
        action: Action
    ): OnboardingState = when (action) {
        is Init -> state.copy(
            isInitializing = true,
            error = null
        )

        is Error -> state.copy(
            error = action.error,
            isInitializing = false
        )

        is SelectModel -> state.copy(
            selectedModel = action.name
        )

        is PageChanged -> state.copy(
            page = OnboardingPage.entries[action.page]
        )

        is Done -> state.copy(
            isInitializing = false
        )

        is Loaded -> state.copy(
            models = action.models,
            isInitializing = false,
            error = null
        )

        else -> state
    }
}
