package com.micrantha.eyespie.features.onboarding.ui

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.arch.StateMapper
import com.micrantha.bluebell.domain.repository.LocalizedRepository
import com.micrantha.bluebell.platform.BackgroundDownloadManager
import com.micrantha.bluebell.platform.Notifications
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.domain.repository.AiRepository
import eyespie.composeapp.generated.resources.download_started
import eyespie.composeapp.generated.resources.downloading

class OnboardingEnvironment(
    private val context: ScreenContext,
    private val downloadManager: BackgroundDownloadManager,
    private val notifications: Notifications,
    private val aiRepository: AiRepository
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
    ) = when(action) {
        is OnboardingAction.Download -> {
            val modelInfo = aiRepository.getCurrentModelInfo()
            downloadManager.startDownload(
                url = modelInfo.url,
                fileName = modelInfo.fileName
            ).let { taskId ->
                notifications.schedule(
                    id = taskId,
                    title = string(S.download_started).getOrThrow(),
                    message = string(S.downloading, "Gemma3 AI Model").getOrThrow(),
                )
            }
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
