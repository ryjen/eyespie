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
import com.micrantha.eyespie.domain.entities.AuthoredClue
import com.micrantha.eyespie.domain.entities.ClueAuthority
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.manualClue as createManualClue
import com.micrantha.eyespie.domain.repository.ClueRepository
import com.micrantha.eyespie.features.scan.entities.ClueAuthoringMode
import com.micrantha.eyespie.features.scan.entities.ScanClue
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.AnalyzedClues
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.GenerateClues
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.GeneratedCluesUnavailable
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.Init
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.LoadError
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.Retry
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SaveScanEdit
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SaveThingError
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.SelectClue
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.UpdateManualAnswer
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.UpdateManualClue
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.UseGeneratedAuthoring
import com.micrantha.eyespie.features.scan.entities.ScanEditAction.UseManualAuthoring
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
            authoringMode = ClueAuthoringMode.CHOOSE,
            isBusy = false,
            isError = false,
        )

        is GenerateClues -> state.copy(
            authoringMode = ClueAuthoringMode.GENERATED,
            generationUnavailable = false,
            isBusy = true,
            isError = false,
        )

        is GeneratedCluesUnavailable -> state.copy(
            authoringMode = ClueAuthoringMode.MANUAL,
            generationUnavailable = true,
            isBusy = false,
            isError = false,
        )

        is UseManualAuthoring -> state.copy(
            authoringMode = ClueAuthoringMode.MANUAL,
            isBusy = false,
            isError = false,
        )

        is UseGeneratedAuthoring -> if (state.clues != null) {
            state.copy(
                authoringMode = ClueAuthoringMode.GENERATED,
                isBusy = false,
                isError = false,
            )
        } else {
            state
        }

        is UpdateManualClue -> state.copy(manualClue = action.value)
        is UpdateManualAnswer -> state.copy(manualAnswer = action.value)

        is SelectClue -> {
            val selected = state.selected?.copy(action.id) {
                it.copy(isSelected = !it.isSelected)
            }
            state.copy(
                selected = selected,
                hasSelected = selected?.values?.any { it.isSelected } ?: false,
            )
        }

        is AnalyzedClues -> state.copy(
            clues = action.value,
            selected = stateMapOf(action.value.clues.mapIndexed { index, clue ->
                index to clue.toScanClue(index)
            }.toMap()),
            authoringMode = ClueAuthoringMode.GENERATED,
            hasSelected = false,
            generationUnavailable = false,
            isBusy = false,
            isError = false,
        )

        is SaveScanEdit -> state.copy(disabled = true)
        is SaveThingError -> state.copy(disabled = false, isBusy = false, isError = true)
        is LoadError -> state.copy(
            disabled = false,
            isBusy = false,
            isError = true,
        )

        is Retry -> state.copy(
            isBusy = state.image == null,
            isError = false,
        )

        else -> state
    }

    override suspend fun invoke(action: Action, state: ScanEditState) {
        when (action) {
            is Init -> loadImage(state)

            is Retry -> if (state.image == null) {
                loadImage(state)
            }

            is GenerateClues -> {
                val path = state.path
                if (path == null) {
                    dispatch(GeneratedCluesUnavailable)
                } else {
                    clueRepository.clues(path).onSuccess {
                        dispatch(AnalyzedClues(it))
                    }.onFailure {
                        dispatch(GeneratedCluesUnavailable)
                    }
                }
            }

            is SaveScanEdit -> state.asProof().onSuccess { proof ->
                val image = state.path
                if (image == null) {
                    dispatch(SaveThingError)
                    return
                }
                uploadCaptureUseCase(
                    proof = proof,
                    image = image,
                ).onSuccess {
                    navigateBack()
                }.onFailure {
                    dispatch(SaveThingError)
                }
            }.onFailure {
                dispatch(SaveThingError)
            }
        }
    }

    override fun map(state: ScanEditState): ScanEditUiState {
        val canSave = when (state.authoringMode) {
            ClueAuthoringMode.CHOOSE -> false
            ClueAuthoringMode.GENERATED -> state.hasSelected
            ClueAuthoringMode.MANUAL -> createManualClue(
                state.manualClue,
                state.manualAnswer,
            ).isSuccess
        }
        return ScanEditUiState(
            image = state.image?.let { BitmapPainter(it.toImageBitmap()) },
            enabled = state.disabled.not() && canSave,
            clues = state.selected?.values ?: emptyList(),
            authoringMode = state.authoringMode,
            manualClue = state.manualClue,
            manualAnswer = state.manualAnswer,
            generationUnavailable = state.generationUnavailable,
            canUseGenerated = state.clues != null,
            isBusy = state.isBusy,
            isError = state.isError,
        )
    }

    private suspend fun loadImage(state: ScanEditState) {
        val path = state.path
        if (path == null) {
            dispatch(LoadError)
            return
        }
        loadCameraImageUseCase(path).onSuccess {
            dispatch(it)
        }.onFailure {
            dispatch(LoadError)
        }
    }

    private fun ScanEditState.asProof(): Result<Proof> = try {
        val authority = when (authoringMode) {
            ClueAuthoringMode.CHOOSE -> throw IllegalStateException("clue authoring is incomplete")
            ClueAuthoringMode.MANUAL -> ClueAuthority(
                listOf(createManualClue(manualClue, manualAnswer).getOrThrow())
            )
            ClueAuthoringMode.GENERATED -> {
                val provenance = requireNotNull(clues).provenance
                val selectedClues = selected?.values
                    ?.filter { it.isSelected }
                    .orEmpty()
                require(selectedClues.isNotEmpty()) { "at least one generated clue must be selected" }
                ClueAuthority(
                    selectedClues.map { clue ->
                        AuthoredClue.Generated(
                            clue = clue.clue,
                            expectedAnswer = clue.answer,
                            confidence = clue.confidence,
                            provenance = provenance,
                        )
                    }
                )
            }
        }
        Result.success(
            Proof(
                clues = authority,
                location = location,
            )
        )
    } catch (error: IllegalArgumentException) {
        Result.failure(error)
    } catch (error: IllegalStateException) {
        Result.failure(error)
    }

    private fun AiClue.toScanClue(id: Int) = ScanClue(
        id = id,
        answer = answer,
        clue = data,
        confidence = confidence,
        isSelected = false,
    )
}
