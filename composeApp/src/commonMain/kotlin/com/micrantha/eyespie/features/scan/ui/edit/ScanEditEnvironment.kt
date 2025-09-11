package com.micrantha.eyespie.features.scan.ui.edit

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
import com.micrantha.eyespie.domain.repository.LocationRepository
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.ClearColor
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.ClearLabel
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.ColorChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.CustomLabelChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.Init
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.LabelChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.LoadError
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.LoadedImage
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.NameChanged
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.SaveScanEdit
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditAction.SaveThingError
import com.micrantha.eyespie.features.scan.ui.usecase.AnalyzeCaptureUseCase
import com.micrantha.eyespie.features.scan.ui.usecase.GetEditCaptureUseCase
import com.micrantha.eyespie.features.scan.ui.usecase.UploadCaptureUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ScanEditEnvironment(
    private val context: ScreenContext,
    private val uploadCaptureUseCase: UploadCaptureUseCase,
    private val getEditCaptureUseCase: GetEditCaptureUseCase,
    private val analyzeCaptureUseCase: AnalyzeCaptureUseCase,
    private val locationRepository: LocationRepository,
    private val currentSession: CurrentSession
) : Reducer<ScanEditState>, Effect<ScanEditState>,
    StateMapper<ScanEditState, ScanEditUiState>,
    Dispatcher by context.dispatcher,
    Router by context.router {

    init {
        analyzeCaptureUseCase.clues.onEach(::dispatch).launchIn(dispatchScope)
        locationRepository.flow().onEach(::dispatch).launchIn(dispatchScope)
    }

    override fun reduce(state: ScanEditState, action: Action) = when (action) {
        is Init -> state.copy(
            path = action.image
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

        is LoadedImage -> state.copy(
            image = action.data
        )

        is ClearLabel -> state.copy(
            customLabel = ""
        )

        is ClearColor -> state.copy(
            customColor = ""
        )

        is CustomLabelChanged -> state.copy(customLabel = action.data)
        else -> state
    }

    override suspend fun invoke(action: Action, state: ScanEditState) {
        when (action) {
            is Init -> {
                analyzeCaptureUseCase(action.image).launchIn(dispatchScope)
                getEditCaptureUseCase(action.image)
                    .onSuccess {
                        dispatch(LoadedImage(it))
                    }.onFailure {
                        Log.e("unable to load image", it)
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
        colors = state.colors?.map {
            Choice(
                label = it.value.display(),
                tag = it.value.data,
                key = it.key
            )
        } ?: emptyList(),
        customColor = state.customColor,
        detections = state.detections?.map {
            Choice(
                label = it.value.display(),
                key = it.value.data,
                tag = it.key
            )
        } ?: emptyList(),
        customDetection = state.customDetection,
        name = state.name ?: "",
        image = state.image,
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
        location = locationRepository.currentLocation()?.point,
        match = embedding,
        image = path!!,
        playerID = currentSession.requirePlayer().id
    )

}
