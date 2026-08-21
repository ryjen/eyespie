package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.mvi.Reducer

object AppReducer : Reducer<AppState, AppIntent> {
    override fun reduce(state: AppState, intent: AppIntent): AppState = when (intent) {
        AppIntent.Refresh -> state.copy(loading = true)
        AppIntent.NavigateHome -> state.copy(
            screen = AppScreen.Home,
            failure = null,
            busy = false,
            latestOutcome = null,
        )
        AppIntent.NavigateCreate -> state.copy(
            screen = AppScreen.Create,
            failure = null,
            busy = false,
            latestOutcome = null,
        )
        is AppIntent.NavigatePlay -> state.copy(
            screen = AppScreen.Play(intent.gameId, intent.thingId),
            failure = null,
            busy = false,
            latestOutcome = null,
        )
        AppIntent.DismissFailure -> state.copy(failure = null)
        AppIntent.CameraFailed -> state.copy(failure = AppFailure.CameraUnavailable)

        is AppIntent.CreateNameChanged -> state.copy(
            createForm = state.createForm.copy(name = intent.value),
        )
        is AppIntent.CreateClueChanged -> state.copy(
            createForm = state.createForm.copy(clue = intent.value),
        )
        is AppIntent.CreateExpectedAnswerChanged -> state.copy(
            createForm = state.createForm.copy(expectedAnswer = intent.value),
        )
        is AppIntent.CreateTargetCaptured -> state.copy(busy = true, failure = null)
        is AppIntent.GuessCaptured -> state.copy(busy = true, failure = null)

        is AppIntent.SnapshotLoaded -> state.copy(
            snapshot = intent.snapshot,
            loading = false,
            failure = null,
        )
        is AppIntent.OperationFailed -> state.copy(
            loading = false,
            busy = false,
            failure = AppFailure.Game(intent.failure),
        )
        is AppIntent.GameCreated -> state.copy(
            snapshot = intent.snapshot,
            screen = AppScreen.Home,
            loading = false,
            busy = false,
            failure = null,
            createForm = CreateGameFormState(),
            latestOutcome = null,
        )
        is AppIntent.GuessCompleted -> state.copy(
            snapshot = intent.snapshot ?: state.snapshot,
            loading = false,
            busy = false,
            failure = null,
            latestOutcome = intent.outcome,
        )
    }
}
