package com.micrantha.eyespie.mvi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseInteractor<State, Intent>(
    initialState: State,
    private val reducer: Reducer<State, Intent>,
) : Interactor<State, Intent> {
    private val mutableState = MutableStateFlow(initialState)
    final override val state: StateFlow<State> = mutableState.asStateFlow()

    final override fun dispatch(intent: Intent) {
        val previousState = mutableState.value
        val nextState = reducer.reduce(previousState, intent)
        mutableState.value = nextState
        afterReduce(intent, previousState, nextState)
    }

    protected abstract fun afterReduce(
        intent: Intent,
        previousState: State,
        stateAfterReduce: State,
    )
}
