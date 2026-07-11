package com.micrantha.eyespie.features.scan.ui.sync

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.Effect
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.features.scan.data.source.CaptureSyncSource
import com.micrantha.eyespie.features.scan.ui.sync.PendingSyncAction.Back
import com.micrantha.eyespie.features.scan.ui.sync.PendingSyncAction.Delete
import com.micrantha.eyespie.features.scan.ui.sync.PendingSyncAction.Load
import com.micrantha.eyespie.features.scan.ui.sync.PendingSyncAction.Loaded

class PendingSyncEnvironment(
    private val context: ScreenContext,
    private val source: CaptureSyncSource
) : Reducer<PendingSyncState>, Effect<PendingSyncState>, Dispatcher by context.dispatcher {

    override fun reduce(state: PendingSyncState, action: Action) = when (action) {
        is Loaded -> state.copy(pending = action.pending)
        else -> state
    }

    override suspend fun invoke(action: Action, state: PendingSyncState) {
        when (action) {
            is Load -> source.getAll().onSuccess {
                dispatch(Loaded(it))
            }
            is Delete -> source.remove(action.id).onSuccess {
                dispatch(Load)
            }
            is Back -> context.router.navigateBack()
        }
    }
}
