package com.micrantha.eyespie.features.onboarding.ui.genai

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.data.DownloadState
import com.micrantha.bluebell.domain.security.sha256
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
import com.micrantha.eyespie.features.onboarding.entities.GenAiDownloadAction.Init
import com.micrantha.eyespie.features.onboarding.entities.GenAiDownloadState
import com.micrantha.eyespie.features.onboarding.entities.GenAiDownloadUiState
import com.micrantha.eyespie.features.onboarding.usecase.LoadModelConfig
import okio.Path.Companion.toPath
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GenAiDownloadScreenModel(
    context: ScreenContext,
    private val platform: Platform,
    private val onboardingRepository: OnboardingRepository,
    private val backgroundDownloader: BackgroundDownloader,
    private val loadModelConfig: LoadModelConfig,
    private val loadMainUseCase: LoadMainUseCase,
): MappedScreenModel<GenAiDownloadState, GenAiDownloadUiState>(context, GenAiDownloadState(), ::map), Reducer<GenAiDownloadState>, Effect<GenAiDownloadState> {

    init {
        store.addReducer(::reduce).applyEffect(::invoke).dispatch(Init)
    }

    override fun reduce(state: GenAiDownloadState, action: Action): GenAiDownloadState {
        return when(action) {
            is DownloadState.Progress -> state.copy(progress = action.progress)
            is Download -> state.copy(name = action.name)
            is Error -> state.copy(error = action.cause)
            else -> state
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(action: Action, state: GenAiDownloadState) {
        when(action) {
            is Init -> onboardingRepository.genAiModel()?.let { model ->
                loadModelConfig().mapCatching { models ->
                    dispatch(Download(model, models[model]!!))
                }.onFailure {
                    dispatch(Error(it))
                }
            }
            is Download -> {
                val id = action.model.url.hashCode().toLong()
                val tag = Uuid.random().toString()
                val filePath = platform.sharedFilesPath().resolve(sha256(action.model.url))
                backgroundDownloader.startDownload(
                    id = id,
                    name = action.name,
                    url = action.model.url,
                    filePath = filePath,
                    tag = tag
                )
                action.model.checksum?.let { checksum ->
                    backgroundDownloader.startDownload(
                        id = action.model.checksum.hashCode().toLong(),
                        name = null,
                        url = checksum,
                        filePath = "${filePath}.sha256".toPath(),
                        tag = tag
                    )
                }
                backgroundDownloader.observe().filterById(id).collect {
                    dispatch(it)
                }
            }
            is Done -> {
                loadMainUseCase().onFailure {
                    dispatch(Error(it))
                }
            }
        }
    }

    companion object {
        fun map(state: GenAiDownloadState)= GenAiDownloadUiState(
            state.progress,
            state.name,
            UiResult.Default
        )
    }
}
