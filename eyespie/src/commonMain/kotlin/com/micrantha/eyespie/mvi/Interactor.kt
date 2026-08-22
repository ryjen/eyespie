package com.micrantha.eyespie.mvi

import kotlinx.coroutines.flow.StateFlow

/**
 * Minimal KMP presentation contract adapted from Achillea's MVI interactor pattern.
 *
 * Dispatch always reduces synchronously before any induced side effect runs.
 */
interface Interactor<State, Intent> {
    val state: StateFlow<State>
    fun dispatch(intent: Intent)
}
