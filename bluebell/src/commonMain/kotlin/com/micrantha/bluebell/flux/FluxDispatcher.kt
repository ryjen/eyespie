package com.micrantha.bluebell.flux

import com.micrantha.bluebell.observability.logger
import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.observability.debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class FluxDispatcher internal constructor(
    override val dispatchScope: CoroutineScope = CoroutineScope(Dispatchers.Default) + Job()
) : Dispatcher, Dispatcher.Registry {
    private val actions = MutableSharedFlow<Action>()
    private val log by logger()

    override fun register(dispatcher: Dispatcher) {
        actions.onEach(dispatcher::send)
            .onEach(log::debug)
            .catch { log.error(it) { "registered dispatch failed" } }
            .launchIn(dispatchScope)
    }

    override fun dispatch(action: Action) {
        dispatchScope.launch {
            actions.emit(action)
        }
    }

    fun cancel() {
        dispatchScope.cancel()
    }

    override suspend fun send(action: Action) {
        actions.emit(action)
    }
}
