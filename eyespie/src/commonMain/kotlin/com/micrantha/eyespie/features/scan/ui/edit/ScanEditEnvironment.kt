package com.micrantha.eyespie.features.scan.ui.edit

import androidx.compose.ui.graphics.painter.BitmapPainter
import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.arch.StateMapper
import com.micrantha.bluebell.domain.stateMapOf
import com.micrantha.bluebell.ui.components.Router
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.domain.entities.AiClue
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.repository.ClueRepository
import com.micrantha.eyespie.features.scan.entities.ScanClue
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.AnalyzedClues
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.Init
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.LoadError
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.Retry
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SaveScanEdit
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SaveThingError
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SelectClue
import com.micrantha.eyespie.features.scan.entities.ScanEditState
import com.micrantha.eyespie.features.scan.entities.ScanEditUiState
import com.micrantha.eyespie.features.scan.usecase.UploadCaptureUseCase
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.LoadCameraImageUseCase

class ScanEditEnvironment(
    private val context: ScreenContext,
    private val uploadCaptureUseCase: UploadCaptureUseCase,
    private val clueRepository: ClueRepository,
    private val loadCameraImageUseCase: LoadCameraImageUseCase
) : Reducer<ScanEditState>, Effect<ScanEditState>,
    StateMapper<ScanEditState, ScanEditUiState>,
    Dispatcher by context.dispatcher,
    Router by context.router {

    override fun reduce(state: ScanEditState, action: Action) = when (action) {
        is Init -> state.copy(
            path = action.params.image,
            location = action.params.location,
        )

        is CameraImage -> state.copy(
            image = action,
            isBusy = true,
        )

        is SelectClue -> state.copy(
            selected = state.selected?.copy(action.id) {
                it.copy(isSelected = !it.isSelected)
            },
            hasSelected = state.selected?.values?.any { it.isSelected } ?: false
        )

        is AnalyzedClues -> state.copy(
            clues = action.value,
            selected = stateMapOf(action.value.mapIndexed { index, clue ->
                index to clue.toScanClue(index)
            }.toMap()),
            isBusy = false,
            isError = false
        )

        is SaveScanEdit -> state.copy(disabled = true)
        is SaveThingError -> state.copy(disabled = false, isError = true)
        is LoadError -> state.copy(
            disabled = false,
            isBusy = false,
            isError = true
        )

        else -> state
    }

    override suspend fun invoke(action: Action, state: ScanEditState) {
        when (action) {
            is Init -> {
                loadCameraImageUseCase(state.path!!).onSuccess {
                    dispatch(it)
                }.onFailure {
                    dispatch(LoadError)
                }
            }

            is CameraImage, Retry ->
                clueRepository.clues(state.path!!).onSuccess {
                    dispatch(AnalyzedClues(it))
                }.onFailure {
                    dispatch(LoadError)
                }

            is SaveScanEdit -> uploadCaptureUseCase(
                proof = state.asProof(),
                image = state.path!!
            ).onSuccess {
                navigateBack()
            }.onFailure {
                dispatch(SaveThingError)
            }
        }
    }

    override fun map(state: ScanEditState) = ScanEditUiState(
        image = state.image?.let { BitmapPainter(it.toImageBitmap()) },
        enabled = state.disabled.not() && state.hasSelected,
        clues = state.selected?.values ?: emptyList(),
        isBusy = state.isBusy,
        isError = state.isError
    )

    private fun ScanEditState.asProof() = Proof(
        clues = clues,
        location = location
    )

    private fun AiClue.toScanClue(id: Int) = ScanClue(
        id,
        answer,
        data,
        false
    )
}
