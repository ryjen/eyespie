package com.micrantha.eyespie.features.onboarding.ui

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.arch.StateMapper
import com.micrantha.bluebell.domain.repository.LocalizedRepository
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.app.ui.usecase.LoadMainUseCase
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.usecase.DownloadModelsUseCase

class OnboardingEnvironment(
    private val context: ScreenContext,
    private val onboardingRepository: OnboardingRepository,
    private val downloadModelsUseCase: DownloadModelsUseCase,
    private val loadMainUseCase: LoadMainUseCase
) : Reducer<OnboardingState>, Effect<OnboardingState>,
    LocalizedRepository by context.i18n {

    override fun reduce(
        state: OnboardingState,
        action: Action
    ): OnboardingState = when (action) {
        else -> state
    }

    override suspend fun invoke(
        action: Action,
        state: OnboardingState
    ) = when (action) {
        is OnboardingAction.Download -> {
            downloadModelsUseCase()
            onboardingRepository.setHasRunOnce()
            loadMainUseCase()
        }

        else -> Unit
    }

    fun map(state: OnboardingState) = OnboardingEnvironment.map(state)

    companion object : StateMapper<OnboardingState, OnboardingUiState> {
        override fun map(state: OnboardingState) = OnboardingUiState(
            page = state.page
        )
    }
}
