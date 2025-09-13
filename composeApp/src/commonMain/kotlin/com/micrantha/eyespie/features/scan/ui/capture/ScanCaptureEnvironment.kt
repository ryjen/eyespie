package com.micrantha.eyespie.features.scan.ui.capture

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.domain.repository.LocalizedRepository
import com.micrantha.bluebell.platform.FileSystem
import com.micrantha.bluebell.ui.components.Router
import com.micrantha.bluebell.ui.components.Router.Options.Replace
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.bluebell.ui.screen.navigate
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.domain.repository.LocationRepository
import com.micrantha.eyespie.features.scan.ui.capture.ScanAction.Back
import com.micrantha.eyespie.features.scan.ui.capture.ScanAction.DoneScan
import com.micrantha.eyespie.features.scan.ui.capture.ScanAction.ScanError
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditParams
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditScreen
import com.micrantha.eyespie.features.scan.ui.usecase.TakeCaptureUseCase
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ScanCaptureEnvironment(
    private val context: ScreenContext,
    private val takeCaptureUseCase: TakeCaptureUseCase,
    locationRepository: LocationRepository,
) : Reducer<ScanState>, Effect<ScanState>,
    Router by context.router,
    FileSystem by context.fileSystem,
    Dispatcher by context.dispatcher,
    LocalizedRepository by context.i18n {

    init {
        locationRepository.flow().onEach(::dispatch).launchIn(dispatchScope)
    }

    override suspend fun invoke(action: Action, state: ScanState) {
        when (action) {

            is DoneScan -> takeCaptureUseCase(
                state.image!!
            ).onSuccess { url ->
                context.navigate<ScanEditScreen, ScanEditParams>(options = Replace, arg = ScanEditParams(url, state.location!!))
            }.onFailure {
                dispatch(ScanError)
            }

            is Back -> context.router.navigateBack()
        }
    }

    override fun reduce(state: ScanState, action: Action) = when (action) {
        is CameraImage -> state.copy(image = action)

        is Location -> state.copy(
            location = action
        )

        is DoneScan -> state.copy(
            busy = true,
            enabled = false,
        )

        is ScanError -> state.copy(
            enabled = true,
            busy = false
        )

        else -> state
    }
}
