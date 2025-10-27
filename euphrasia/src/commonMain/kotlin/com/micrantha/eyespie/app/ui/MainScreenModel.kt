package com.micrantha.eyespie.app.ui

import com.micrantha.bluebell.observability.logger
import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.observability.debug
import com.micrantha.bluebell.ui.screen.ContextualScreenModel
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.app.ui.MainAction.Load
import com.micrantha.eyespie.app.usecase.LoadMainUseCase
import kotlinx.coroutines.launch

class MainScreenModel(
    context: ScreenContext,
    private val loadMainUseCase: LoadMainUseCase
) : ContextualScreenModel(context) {

    private val log by logger()

    override fun dispatch(action: Action) {
        dispatchScope.launch {
            send(action)
        }
    }

    override suspend fun send(action: Action) {
        when (action) {
            is Load -> loadMainUseCase()
            else -> log.debug("unknown action $action")
        }
    }
}
