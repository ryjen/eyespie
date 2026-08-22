package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.mvi.Reducer

object PlayGameReducer : Reducer<PlayGameState, PlayGameIntent> {
    override fun reduce(state: PlayGameState, intent: PlayGameIntent): PlayGameState = when (intent) {
        PlayGameIntent.Load -> state.copy(loading = true, failure = null)
        is PlayGameIntent.ContentLoaded -> state.copy(
            content = intent.content,
            loading = false,
            busy = false,
            failure = null,
            latestOutcome = null,
        )
        is PlayGameIntent.LoadFailed -> state.copy(
            loading = false,
            busy = false,
            failure = PlayGameFailure.Game(intent.failure),
        )
        is PlayGameIntent.GuessCaptured -> if (state.busy || state.matched) {
            state
        } else {
            state.copy(
                busy = true,
                failure = null,
                latestOutcome = null,
                guessGeneration = state.guessGeneration + 1,
            )
        }
        is PlayGameIntent.GuessCompleted -> if (intent.generation == state.guessGeneration && state.busy) {
            state.copy(
                busy = false,
                failure = null,
                latestOutcome = intent.outcome,
            )
        } else {
            state
        }
        is PlayGameIntent.OperationFailed -> if (intent.generation == state.guessGeneration && state.busy) {
            state.copy(
                busy = false,
                failure = PlayGameFailure.Game(intent.failure),
            )
        } else {
            state
        }
        PlayGameIntent.CameraFailed -> if (state.busy || state.matched) state else state.copy(failure = PlayGameFailure.CameraUnavailable)
        PlayGameIntent.DismissFailure -> state.copy(failure = null)
        PlayGameIntent.NextClueSelected,
        PlayGameIntent.Back -> state
    }
}
