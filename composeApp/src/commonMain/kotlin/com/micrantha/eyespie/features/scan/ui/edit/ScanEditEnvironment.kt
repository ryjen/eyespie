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
import com.micrantha.eyespie.core.ui.component.Choice
import com.micrantha.eyespie.core.ui.component.updateKey
import com.micrantha.eyespie.domain.entities.Clues
import com.micrantha.eyespie.domain.entities.LabelClue
import com.micrantha.eyespie.domain.entities.LocationClue
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.repository.ClueRepository
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.ClearColor
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.ClearLabel
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.ColorChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.CustomColorChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.CustomDetectionChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.CustomLabelChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.Init
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.LabelChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.LoadError
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.NameChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.SaveScanEdit
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.SaveThingError
import com.micrantha.eyespie.features.scan.usecase.UploadCaptureUseCase

class ScanEditEnvironment(
    private val context: ScreenContext,
    private val uploadCaptureUseCase: UploadCaptureUseCase,
    private val clueRepository: ClueRepository,
    private val currentSession: CurrentSession,
) : Reducer<ScanEditState>, Effect<ScanEditState>,
    StateMapper<ScanEditState, ScanEditUiState>,
    Dispatcher by context.dispatcher,
    Router by context.router {

    override fun reduce(state: ScanEditState, action: Action) = when (action) {
        is Init -> state.copy(
            image = action.params.image,
            location = action.params.location,
            hasAI = true //aiRepository.isReady()
        )

        is LabelClue -> state.copy(
            labels = state.labels?.updateKey(action.display()) { clue ->
                clue.copy(data = action.data, confidence = action.confidence)
            }
        )

        is LabelChanged -> state.copy(
            customLabel = null,
            labels = state.labels?.updateKey(action.data.key) { clue ->
                clue.copy(data = action.data.tag)
            }
        )

        is ColorChanged -> state.copy(
            colors = state.colors?.updateKey(action.data.key) { clue ->
                clue.copy(data = action.data.tag)
            }
        )

        is NameChanged -> state.copy(
            name = action.data
        )

        is ClearLabel -> state.copy(
            customLabel = ""
        )

        is ClearColor -> state.copy(
            customColor = ""
        )

        is ScanEditAction.AnalyzedClues -> state.copy(
            labels = action.clues.labels?.associateBy { it.data }?.toMutableMap(),
            colors = action.clues.colors?.associateBy { it.data }?.toMutableMap(),
            detections = action.clues.detections?.associateBy { it.data }?.toMutableMap(),
        )

        is CustomLabelChanged -> state.copy(customLabel = action.data)
        is CustomColorChanged -> state.copy(customColor = action.data)
        is CustomDetectionChanged -> state.copy(customDetection = action.data)
        is SaveScanEdit -> state.copy(disabled = true)
        is SaveThingError -> state.copy(disabled = false)
        is LoadError -> state.copy(disabled = false)
        else -> state
    }

    override suspend fun invoke(action: Action, state: ScanEditState) {
        when (action) {
            is Init -> {
                clueRepository.generate(state.image!!).onSuccess {
                    dispatch(ScanEditAction.AnalyzedClues(it))
                }.onFailure {
                    dispatch(LoadError)
                }
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
        labels = state.labels?.map {
            Choice(
                label = it.value.display(),
                tag = it.value.data,
                key = it.key
            )
        } ?: emptyList(),
        customLabel = state.customLabel,
        showLabels = state.labels?.isNotEmpty() ?: state.customLabel?.isNotBlank()
        ?: state.hasAI.not(),
        colors = state.colors?.map {
            Choice(
                label = it.value.display(),
                tag = it.value.data,
                key = it.key
            )
        } ?: emptyList(),
        customColor = state.customColor,
        showColors = state.colors?.isNotEmpty() ?: state.customColor?.isNotBlank()
        ?: state.hasAI.not(),
        detections = state.detections?.map {
            Choice(
                label = it.value.display(),
                key = it.value.data,
                tag = it.key
            )
        } ?: emptyList(),
        customDetection = state.customDetection,
        showDetections = state.detections?.isNotEmpty() ?: state.customDetection?.isNotBlank()
        ?: state.hasAI.not(),
        name = state.name ?: "",
        image = state.image?.let { BitmapPainter(it.toImageBitmap()) },
        enabled = state.disabled.not()
    )

    private fun ScanEditState.asProof() = Proof(
        clues = Clues(
            labels = labels?.values?.toSet(),
            colors = colors?.values?.toSet(),
            detections = detections?.values?.toSet(),
            location = location?.data?.let { LocationClue(it) }
        ),
        name = name,
        location = location?.point,
        match = embedding,
        image = path!!,
        playerID = currentSession.requirePlayer().id
    )

}
