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
import com.micrantha.eyespie.features.scan.entities.ScanAction.Back
import com.micrantha.eyespie.features.scan.entities.ScanAction.ScanError
import com.micrantha.eyespie.features.scan.entities.ScanEditParams
import com.micrantha.eyespie.features.scan.entities.ScanState
import com.micrantha.eyespie.features.scan.ui.edit.ScanEditScreen
import okio.Path

class ScanCaptureEnvironment(
    private val context: ScreenContext,
) : Reducer<ScanState>, Effect<ScanState>,
    Router by context.router,
    FileSystem by context.fileSystem,
    Dispatcher by context.dispatcher,
    LocalizedRepository by context.i18n {

    override suspend fun invoke(action: Action, state: ScanState) {
        when (action) {
            is Path -> try {
                navigate(
                    ScanEditScreen(ScanEditParams(action, state.location!!)),
                    Router.Options.Replace
                )
            } catch (_: Throwable) {
                dispatch(ScanError)
            }

            is Back -> context.router.navigateBack()
        }
    }

    override fun reduce(state: ScanState, action: Action) = when (action) {

        is Location -> state.copy(
            location = action
        )

        is Path -> state.copy(
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
