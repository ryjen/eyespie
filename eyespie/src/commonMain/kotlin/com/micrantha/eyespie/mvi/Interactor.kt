package com.micrantha.eyespie.mvi

/**
 * Minimal KMP presentation contract adapted from Achillea's MVI interactor pattern.
 *
 * Dispatch always reduces synchronously before any induced side effect runs.
 */
interface Interactor<State, Intent> : StateReader<State>, IntentDispatcher<Intent>
