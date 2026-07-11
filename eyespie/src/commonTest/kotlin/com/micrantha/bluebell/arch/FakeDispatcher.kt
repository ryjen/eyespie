package com.micrantha.bluebell.arch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

class FakeDispatcher(
    override val dispatchScope: CoroutineScope = MainScope()
) : Dispatcher {
    val actions = mutableListOf<Action>()
    
    override fun dispatch(action: Action) {
        actions.add(action)
    }

    override suspend fun send(action: Action) {
        actions.add(action)
    }
}
