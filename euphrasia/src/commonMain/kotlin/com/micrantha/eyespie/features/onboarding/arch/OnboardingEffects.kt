package com.micrantha.eyespie.features.onboarding.arch

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.domain.security.sha256
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.app.ui.usecase.LoadMainUseCase
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.*
import com.micrantha.eyespie.features.onboarding.entities.OnboardingPage
import com.micrantha.eyespie.features.onboarding.entities.OnboardingState
import com.micrantha.eyespie.features.onboarding.usecase.DownloadModelUseCase
import com.micrantha.eyespie.features.onboarding.usecase.LoadModelConfigUseCase

class OnboardingEffects(
    private val context: ScreenContext,
    private val onboardingRepository: OnboardingRepository,
    private val loadMainUseCase: LoadMainUseCase,
    private val loadModelConfig: LoadModelConfigUseCase,
    private val downloadModelUseCase: DownloadModelUseCase
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

            is Download -> state.selectedModel?.let { model ->
                val download = state.models?.get(model)!!
                downloadModelUseCase(model, download)
                    .onSuccess {
                        onboardingRepository.setHasGenAI(sha256(download.url))
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
