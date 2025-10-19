package com.micrantha.eyespie.features.onboarding.arch

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.ext.getIf
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.app.usecase.LoadMainUseCase
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.Done
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.Download
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.Error
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.Init
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.Loaded
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.NextPage
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.PageChanged
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.SkipDownload
import com.micrantha.eyespie.features.onboarding.entities.OnboardingPage
import com.micrantha.eyespie.features.onboarding.entities.OnboardingState
import com.micrantha.eyespie.features.onboarding.usecase.LoadModelConfig
import com.micrantha.eyespie.features.onboarding.usecase.StartModelDownload

class OnboardingEffects(
    private val context: ScreenContext,
    private val onboardingRepository: OnboardingRepository,
    private val loadMainUseCase: LoadMainUseCase,
    private val loadModelConfig: LoadModelConfig,
    private val startModelDownload: StartModelDownload
) : Effect<OnboardingState>, Dispatcher by context.dispatcher {

    override suspend fun invoke(
        action: Action,
        state: OnboardingState
    ) {
        when (action) {
            is Init -> {
                loadModelConfig().onSuccess {
                    dispatch(Loaded(it))
                }.onFailure {
                    dispatch(Error(it))
                }
            }

            is SkipDownload -> {
                onboardingRepository.setHasGenAI(false)
                dispatch(NextPage)
            }

            is Download -> getIf(state.models, state.selectedModel)?.let { (model, download) ->
                onboardingRepository.setHasGenAI(true)
                startModelDownload(model, download)
                    .onSuccess {
                        onboardingRepository.setModelFile(it)
                        dispatch(NextPage)
                    }.onFailure {
                        dispatch(Error(it))
                    }
            }

            is Done -> {
                onboardingRepository.setHasRunOnce()
                loadMainUseCase().onFailure {
                    dispatch(Error(it))
                }
            }

            is NextPage -> {
                val next = OnboardingPage.entries.getOrNull(state.page.ordinal + 1)
                if (next == null) {
                    dispatch(Done)
                } else {
                    dispatch(PageChanged(next.ordinal))
                }
            }

            else -> Unit
        }
    }
}
