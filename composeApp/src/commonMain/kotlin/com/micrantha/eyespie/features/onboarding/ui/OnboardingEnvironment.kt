package com.micrantha.eyespie.features.onboarding.ui

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.arch.StateMapper
import com.micrantha.bluebell.domain.repository.LocalizedRepository
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.app.ui.usecase.LoadMainUseCase
import com.micrantha.eyespie.domain.repository.AiRepository
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.usecase.DownloadModelsUseCase

class OnboardingEnvironment(
    private val context: ScreenContext,
    private val onboardingRepository: OnboardingRepository,
    private val downloadModelsUseCase: DownloadModelsUseCase,
    private val loadMainUseCase: LoadMainUseCase,
    private val aiRepository: AiRepository
) : Reducer<OnboardingState>, Effect<OnboardingState>,
    LocalizedRepository by context.i18n, Dispatcher by context.dispatcher {

    override fun reduce(
        state: OnboardingState,
        action: Action
    ): OnboardingState = when (action) {
        is OnboardingAction.Error -> state.copy(
            error = action.error,
            isDownloading = false
        )

        is OnboardingAction.PageChanged -> state.copy(
            page = OnboardingPage.entries[action.page]
        )

        is OnboardingAction.LoadedModels -> state.copy(
            models = action.models,
            isDownloading = false,
        )

        is OnboardingAction.SelectedModel -> state.copy(
            selectedModel = action.model,
            isDownloading = true,
            error = null
        )

        is OnboardingAction.Done -> state.copy(
            isDownloading = false
        )

        else -> state
    }

    override suspend fun invoke(
        action: Action,
        state: OnboardingState
    ) {
        when (action) {
            is OnboardingAction.Init -> {
                aiRepository.listModels().onSuccess {
                    dispatch(OnboardingAction.LoadedModels(it))
                }.onFailure {
                    dispatch(OnboardingAction.Error(it))
                }
            }

            is OnboardingAction.Download -> {
                downloadModelsUseCase().onSuccess {
                    dispatch(OnboardingAction.NextPage)
                }.onFailure {
                    dispatch(OnboardingAction.Error(it))
                }
            }

            is OnboardingAction.SelectedModel -> {
                aiRepository.downloadModel(action.model).onSuccess {
                    dispatch(OnboardingAction.Done)
                }.onFailure {
                    dispatch(OnboardingAction.Error(it))
                }
            }

            is OnboardingAction.Done -> {
                onboardingRepository.setHasRunOnce()
                loadMainUseCase()
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
            isBusy = state.isDownloading,
            isError = state.error != null,
            page = state.page,
            models = state.models,
            selectedModel = state.selectedModel
        )
    }
}
