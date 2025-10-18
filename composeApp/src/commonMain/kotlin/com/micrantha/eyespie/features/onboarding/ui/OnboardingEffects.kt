package com.micrantha.bluebell.com.micrantha.eyespie.features.onboarding.ui

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.app.ui.usecase.LoadMainUseCase
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingAction
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingPage
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingState
import com.micrantha.eyespie.features.onboarding.domain.usecase.DownloadModelUseCase
import com.micrantha.eyespie.features.onboarding.domain.usecase.InitGenAIUseCase
import com.micrantha.eyespie.features.onboarding.domain.usecase.LoadModelConfigUseCase

class OnboardingEffects(
    private val context: ScreenContext,
    private val onboardingRepository: OnboardingRepository,
    private val loadMainUseCase: LoadMainUseCase,
    private val initGenAIUseCase: InitGenAIUseCase,
    private val loadModelConfig: LoadModelConfigUseCase,
    private val downloadModelUseCase: DownloadModelUseCase
) : Effect<OnboardingState>, Dispatcher by context.dispatcher {
    override suspend fun invoke(
        action: Action,
        state: OnboardingState
    ) {
        when (action) {
            is OnboardingAction.Init -> {
                loadModelConfig().onSuccess {
                    dispatch(OnboardingAction.Loaded(it))
                }.onFailure {
                    dispatch(OnboardingAction.Error(it))
                }
            }

            is OnboardingAction.SelectModel -> {
                downloadModelUseCase(action.name, state.models?.get(action.name)!!)
                    .onSuccess {
                        dispatch(OnboardingAction.StartGenAI)
                    }.onFailure {
                        dispatch(OnboardingAction.Error(it))
                    }
            }

            is OnboardingAction.StartGenAI -> {
                initGenAIUseCase(state.selectedModel!!).onSuccess {
                    onboardingRepository.setHasGenAI()
                    dispatch(OnboardingAction.Done)
                }.onFailure {
                    dispatch(OnboardingAction.Error(it))
                }
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
}
