package com.micrantha.eyespie.features.scan.ui.capture

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.domain.repository.LocalizedRepository
import com.micrantha.bluebell.platform.FileSystem
import com.micrantha.bluebell.ui.components.Router
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.features.scan.ui.capture.ScanAction.Back
import com.micrantha.eyespie.features.scan.ui.capture.ScanAction.SaveScan
import com.micrantha.eyespie.features.scan.ui.capture.ScanAction.ScanError
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditParams
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditScreen
import com.micrantha.eyespie.features.scan.ui.usecase.TakeCaptureUseCase
import com.micrantha.eyespie.platform.scan.CameraImage

class ScanCaptureEnvironment(
    private val context: ScreenContext,
    private val takeCaptureUseCase: TakeCaptureUseCase,
) : Reducer<ScanState>, Effect<ScanState>,
    Router by context.router,
    FileSystem by context.fileSystem,
    Dispatcher by context.dispatcher,
    LocalizedRepository by context.i18n {

    override suspend fun invoke(action: Action, state: ScanState) {
        when (action) {

            is SaveScan -> takeCaptureUseCase(
                state.image!!
            ).onSuccess { url ->
                state.location?.let { location ->
                    navigate(
                        ScanEditScreen(context, ScanEditParams(url, location)),
                        Router.Options.Replace
                    )
                }
                dispatch(SaveScan)
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

        is SaveScan -> state.copy(
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
