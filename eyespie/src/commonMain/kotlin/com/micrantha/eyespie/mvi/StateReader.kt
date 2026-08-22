package com.micrantha.eyespie.mvi

import kotlinx.coroutines.flow.StateFlow

interface StateReader<State> {
    val state: StateFlow<State>
}
