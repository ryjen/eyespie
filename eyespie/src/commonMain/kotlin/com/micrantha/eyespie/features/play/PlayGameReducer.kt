package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.mvi.Reducer

object PlayGameReducer : Reducer<PlayGameState, PlayGameIntent> {
    override fun reduce(state: PlayGameState, intent: PlayGameIntent): PlayGameState = when (intent) {
        PlayGameIntent.Load -> state.copy(loading = true, failure = null)
        is PlayGameIntent.ContentLoaded -> state.copy(
            content = intent.content,
            loading = false,
            failure = null,
        )
        is PlayGameIntent.LoadFailed -> state.copy(
            loading = false,
            failure = PlayGameFailure.Game(intent.failure),
        )
        is PlayGameIntent.GuessCaptured -> state.copy(busy = true, failure = null)
        is PlayGameIntent.GuessCompleted -> state.copy(
            busy = false,
            failure = null,
            latestOutcome = intent.outcome,
        )
        is PlayGameIntent.OperationFailed -> state.copy(
            busy = false,
            failure = PlayGameFailure.Game(intent.failure),
        )
        PlayGameIntent.CameraFailed -> state.copy(failure = PlayGameFailure.CameraUnavailable)
        PlayGameIntent.DismissFailure -> state.copy(failure = null)
        PlayGameIntent.Back -> state
    }
}
