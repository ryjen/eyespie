package com.micrantha.eyespie.features.scan.ui.sync

import com.micrantha.bluebell.arch.Store
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.bluebell.ui.screen.StatefulScreenModel

class PendingSyncScreenModel(
    context: ScreenContext,
    environment: PendingSyncEnvironment
) : StatefulScreenModel<PendingSyncState>(context, PendingSyncState()) {

    init {
        store.addReducer(environment::reduce).applyEffect(environment)
    }
}
