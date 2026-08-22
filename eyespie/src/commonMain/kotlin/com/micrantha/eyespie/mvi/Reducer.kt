package com.micrantha.eyespie.mvi

/** Pure synchronous state transition. */
fun interface Reducer<State, Intent> {
    fun reduce(state: State, intent: Intent): State
}
