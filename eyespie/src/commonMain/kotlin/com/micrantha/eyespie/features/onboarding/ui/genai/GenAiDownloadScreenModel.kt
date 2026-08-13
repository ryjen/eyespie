package com.micrantha.eyespie.features.onboarding.ui.genai

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.data.DownloadState
import com.micrantha.bluebell.platform.BackgroundDownloader
import com.micrantha.bluebell.platform.Platform
import com.micrantha.bluebell.platform.filterById
import com.micrantha.bluebell.ui.model.UiResult
import com.micrantha.bluebell.ui.screen.MappedScreenModel
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.app.usecase.LoadMainUseCase
import com.micrantha.eyespie.features.onboarding.data.OnboardingRepository
import com.micrantha.eyespie.features.onboarding.entities.GenAiDownloadAction.Done
import com.micrantha.eyespie.features.onboarding.entities.GenAiDownloadAction.Download
import com.micrantha.eyespie.features.onboarding.entities.GenAiDownloadAction.Error
import com.micrantha.eyespie.features.onboarding.entities.GenAiDownloadAction.Init
import com.micrantha.eyespie.features.onboarding.entities.GenAiDownloadAction.Verified
import com.micrantha.eyespie.features.onboarding.entities.GenAiDownloadState
import com.micrantha.eyespie.features.onboarding.entities.GenAiDownloadUiState
import com.micrantha.eyespie.features.onboarding.usecase.LoadModelConfig
import com.micrantha.eyespie.features.onboarding.usecase.ModelIntegrityVerifier
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GenAiDownloadScreenModel(
    context: ScreenContext,
    private val platform: Platform,
    private val onboardingRepository: OnboardingRepository,
    private val backgroundDownloader: BackgroundDownloader,
    private val loadModelConfig: LoadModelConfig,
    private val loadMainUseCase: LoadMainUseCase,
    private val modelIntegrityVerifier: ModelIntegrityVerifier,
) : MappedScreenModel<GenAiDownloadState, GenAiDownloadUiState>(
    context,
    GenAiDownloadState(),
    ::map
), Reducer<GenAiDownloadState>, Effect<GenAiDownloadState> {

    init {
        store.addReducer(::reduce).applyEffect(::invoke).dispatch(Init)
    }

    override fun reduce(state: GenAiDownloadState, action: Action): GenAiDownloadState {
        return when (action) {
            is DownloadState.Progress -> state.copy(progress = action.progress.coerceAtMost(99))
            is DownloadState.Started -> state.copy(progress = 0, error = null)
            is DownloadState.Completed -> state.copy(progress = 99)
            is Verified -> state.copy(progress = 100, error = null)
            is DownloadState.Failed -> state.copy(error = action.throwable ?: action.error)
            is Download -> state.copy(
                model = action.model,
                name = action.name,
                error = null,
                progress = 0
            )

            is Error -> state.copy(error = action.cause)
            else -> state
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(action: Action, state: GenAiDownloadState) {
        when (action) {
            is Init -> onboardingRepository.genAiModel()?.let { model ->
                loadModelConfig().mapCatching { models ->
                    dispatch(Download(model, models[model]!!))
                }.onFailure {
                    dispatch(Error(it))
                }
            }

            is Download -> {
                val checksumPolicy = modelIntegrityVerifier.validateExpectedChecksum(action.model)
                if (checksumPolicy.isFailure) {
                    dispatch(Error(checksumPolicy.exceptionOrNull()!!))
                    return
                }

                val id = action.model.id
                val tag = Uuid.random().toString()
                val filePath =
                    platform.sharedFilesPath().resolve("${action.model.fileName()}.litertlm")
                backgroundDownloader.startDownload(
                    id = id,
                    name = action.name,
                    url = action.model.url,
                    filePath = filePath,
                    tag = tag
                )
                backgroundDownloader.observe().filterById(id).collect {
                    dispatch(it)
                }
            }

            is DownloadState.Completed -> {
                val model = state.model
                if (model == null) {
                    dispatch(Error(IllegalStateException("download completed without a model")))
                    return
                }

                val filePath = platform.sharedFilesPath().resolve("${model.fileName()}.litertlm")
                modelIntegrityVerifier.verify(model, filePath)
                    .onSuccess { dispatch(Verified) }
                    .onFailure { dispatch(Error(it)) }
            }

            is Done -> {
                loadMainUseCase().onFailure {
                    dispatch(Error(it))
                }
            }
        }
    }

    companion object {
        fun map(state: GenAiDownloadState) = GenAiDownloadUiState(
            state.progress,
            state.name,
            when {
                state.error != null -> UiResult.Failure()
                state.progress in 1..99 -> UiResult.Busy()
                state.progress >= 100 -> UiResult.Ready(Unit)
                else -> UiResult.Default
            }
        )
    }
}
