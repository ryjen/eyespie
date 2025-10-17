package com.micrantha.eyespie.features.onboarding.ui

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.arch.StateMapper
import com.micrantha.bluebell.domain.repository.LocalizedRepository
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.app.ui.usecase.LoadMainUseCase
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository

class OnboardingEnvironment(
    private val context: ScreenContext,
    private val onboardingRepository: OnboardingRepository,
    private val loadMainUseCase: LoadMainUseCase,
) : Reducer<OnboardingState>, Effect<OnboardingState>,
    LocalizedRepository by context.i18n, Dispatcher by context.dispatcher {

    override fun reduce(
        state: OnboardingState,
        action: Action
    ): OnboardingState = when (action) {
        is OnboardingAction.Error -> state.copy(
            error = action.error,
            isInitializing = false
        )

        is OnboardingAction.StartGenAI -> state.copy(
            isInitializing = true,
            error = null,
        )

        is OnboardingAction.PageChanged -> state.copy(
            page = OnboardingPage.entries[action.page]
        )

        is OnboardingAction.Done -> state.copy(
            isInitializing = false
        )

        else -> state
    }

    override suspend fun invoke(
        action: Action,
        state: OnboardingState
    ) {
        when (action) {
            is OnboardingAction.StartGenAI -> {
               onboardingRepository.setHasGenAI()
            }

            is OnboardingAction.Done -> {
                onboardingRepository.setHasRunOnce()
                loadMainUseCase().onFailure {
                    dispatch(OnboardingAction.Error(it))
                }
            }

            is OnboardingAction.NextPage -> {
                val next = OnboardingPage.entries.getOrNull(state.page.ordinal + 1)
                if (next == null) {
                    dispatch(OnboardingAction.Done)
                } else {
                    dispatch(OnboardingAction.PageChanged(next.ordinal))
                }
            }

            else -> Unit
        }
    }

    fun map(state: OnboardingState) = OnboardingEnvironment.map(state)

    companion object : StateMapper<OnboardingState, OnboardingUiState> {
        override fun map(state: OnboardingState) = OnboardingUiState(
            isBusy = state.isInitializing,
            isError = state.error != null,
            page = state.page,
        )
    }
}
