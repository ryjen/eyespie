package com.micrantha.eyespie.features.onboarding.ui

import com.micrantha.bluebell.arch.StateMapper
import com.micrantha.bluebell.domain.repository.LocalizedRepository
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingState
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingUiState

class OnboardingMapper(
    private val context: ScreenContext,
) : LocalizedRepository by context.i18n {

    fun map(state: OnboardingState) = OnboardingMapper.map(state)

    companion object : StateMapper<OnboardingState, OnboardingUiState> {
        override fun map(state: OnboardingState) = OnboardingUiState(
            isBusy = state.isInitializing,
            isError = state.error != null,
            page = state.page,
            models = state.models?.map {
                OnboardingUiState.Model(
                    it.key, it.key == state.selectedModel
                )
            } ?: emptyList()
        )
    }
}
