package com.micrantha.eyespie.features.clueauthoring

import com.micrantha.eyespie.mvi.Reducer

object ClueAuthoringReducer : Reducer<ClueAuthoringState, ClueAuthoringIntent> {
    override fun reduce(
        state: ClueAuthoringState,
        intent: ClueAuthoringIntent,
    ): ClueAuthoringState = when (intent) {
        is ClueAuthoringIntent.ClueChanged -> if (state.busy) state else state.copy(clue = intent.value)
        is ClueAuthoringIntent.ExpectedAnswerChanged -> if (state.busy) state else state.copy(expectedAnswer = intent.value)
        ClueAuthoringIntent.CaptureStarted,
        is ClueAuthoringIntent.TargetCaptured -> if (state.busy) state else state.copy(busy = true, failure = null)
        ClueAuthoringIntent.CameraFailed -> if (state.busy) state else state.copy(failure = ClueAuthoringFailure.CameraUnavailable)
        ClueAuthoringIntent.DismissFailure -> state.copy(failure = null)
        ClueAuthoringIntent.Back,
        ClueAuthoringIntent.Added -> ClueAuthoringState()
        is ClueAuthoringIntent.OperationFailed -> state.copy(
            busy = false,
            failure = ClueAuthoringFailure.Game(intent.failure),
        )
    }
}
