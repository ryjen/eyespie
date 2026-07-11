package com.micrantha.eyespie.features.scan.ui.capture

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.ui.components.Router
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.bluebell.ui.screen.navigate
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
    Dispatcher by context.dispatcher {

    override suspend fun invoke(action: Action, state: ScanState) {
        when (action) {
            is Path -> try {
                context.navigate<ScanEditScreen, ScanEditParams>(
                    options = Router.Options.Replace,
                    arg = ScanEditParams(action, state.location!!)
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
