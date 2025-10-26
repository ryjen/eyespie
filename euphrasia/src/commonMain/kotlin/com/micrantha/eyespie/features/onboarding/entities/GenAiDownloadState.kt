package com.micrantha.eyespie.features.onboarding.entities

import com.micrantha.bluebell.ui.model.UiResult

data class GenAiDownloadState(
    val model: AiModel? = null,
    val name: String? = null,
    val progress: Int = 0,
    val error: Throwable? = null,
)

data class GenAiDownloadUiState(
     val progress: Int,
     val name: String?,
     val status: UiResult<Unit>
)

sealed interface GenAiDownloadAction {
    data object Init : GenAiDownloadAction
    data class Download(val name: String, val model: AiModel): GenAiDownloadAction
    data object Done : GenAiDownloadAction
}
