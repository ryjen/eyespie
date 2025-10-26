package com.micrantha.eyespie.features.scan.ui.edit

import androidx.compose.ui.graphics.painter.BitmapPainter
import com.micrantha.bluebell.app.Log
import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.arch.StateMapper
import com.micrantha.bluebell.ui.components.Router
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.domain.entities.Clues
import com.micrantha.eyespie.domain.entities.LocationClue
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.repository.ClueRepository
import com.micrantha.eyespie.features.scan.entities.ScanEditAction
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.Init
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.LoadError
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SaveScanEdit
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SaveThingError
import com.micrantha.eyespie.features.scan.entities.ScanEditState
import com.micrantha.eyespie.features.scan.entities.ScanEditUiState
import com.micrantha.eyespie.features.scan.usecase.UploadCaptureUseCase
import com.micrantha.eyespie.platform.scan.CameraImage
import com.micrantha.eyespie.platform.scan.LoadCameraImageUseCase

class ScanEditEnvironment(
    private val context: ScreenContext,
    private val uploadCaptureUseCase: UploadCaptureUseCase,
    private val clueRepository: ClueRepository,
    private val currentSession: CurrentSession,
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

        is ScanEditAction.SelectClue -> state.copy(
            selected = state.selected?.apply { add(action.index) } ?: mutableSetOf(action.index)
        )

        is ScanEditAction.AnalyzedClues -> state.copy(
            clues = action.proof.toMutableSet(),
            isBusy = false,
        )

        is SaveScanEdit -> state.copy(disabled = true)
        is SaveThingError -> state.copy(disabled = false)
        is LoadError -> state.copy(
            disabled = false,
            isBusy = false,
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

            is CameraImage ->
                clueRepository.clues(state.path!!).onSuccess {
                    dispatch(ScanEditAction.AnalyzedClues(it))
                }.onFailure {
                    Log.e("ScanEdit", it)
                    dispatch(LoadError)
                }

            is SaveScanEdit -> uploadCaptureUseCase(
                state.asProof()
            ).onSuccess {
                navigateBack()
            }.onFailure {
                Log.e("saving scan", it)
                dispatch(SaveThingError)
            }
        }
    }

    override fun map(state: ScanEditState) = ScanEditUiState(
        image = state.image?.let { BitmapPainter(it.toImageBitmap()) },
        enabled = state.disabled.not() && state.selected.isNullOrEmpty().not(),
        clues = state.clues?.mapIndexed { index, clue ->
            ScanEditUiState.Clue(
                answer = clue.answer,
                clue = clue.data,
                isSelected = state.selected?.contains(index) ?: false
            )
        } ?: emptyList(),
        isBusy = state.isBusy,
    )

    private fun ScanEditState.asProof() = Proof(
        clues = Clues(
            clues = clues?.toSet(),
            location = location?.data?.let { LocationClue(it) }
        ),
        name = name,
        location = location?.point,
        match = embedding,
        image = path!!,
        playerID = currentSession.requirePlayer().id
    )

}
