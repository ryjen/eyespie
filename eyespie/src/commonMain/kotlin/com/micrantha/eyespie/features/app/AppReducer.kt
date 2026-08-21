package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.mvi.Reducer

object AppReducer : Reducer<AppState, AppIntent> {
    override fun reduce(state: AppState, intent: AppIntent): AppState = when (intent) {
        AppIntent.Refresh -> state.copy(loading = true)
        AppIntent.NavigateHome -> state.copy(
            screen = AppScreen.Home,
            failure = null,
            busy = false,
            playGame = null,
            playThing = null,
            latestOutcome = null,
        )
        AppIntent.NavigateCreate -> state.copy(
            screen = AppScreen.Create,
            failure = null,
            busy = false,
            playGame = null,
            playThing = null,
            latestOutcome = null,
        )
        is AppIntent.NavigatePlay -> state.selectPlayTarget(
            screen = AppScreen.Play(intent.gameId, intent.thingId),
            snapshot = state.snapshot,
        ).copy(
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

        is AppIntent.SnapshotLoaded -> state.withSnapshot(intent.snapshot).copy(
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
            playGame = null,
            playThing = null,
            latestOutcome = null,
        )
        is AppIntent.GuessCompleted -> {
            val refreshed = intent.snapshot?.let(state::withSnapshot) ?: state
            refreshed.copy(
                loading = false,
                busy = false,
                failure = null,
                latestOutcome = intent.outcome,
            )
        }
    }
}

private fun AppState.withSnapshot(snapshot: LocalGameSnapshot): AppState = when (val current = screen) {
    is AppScreen.Play -> selectPlayTarget(current, snapshot)
    else -> copy(snapshot = snapshot)
}

private fun AppState.selectPlayTarget(
    screen: AppScreen.Play,
    snapshot: LocalGameSnapshot?,
): AppState {
    val game = snapshot?.games?.firstOrNull { it.id == screen.gameId }
    val thing = game?.things?.firstOrNull { it.id == screen.thingId }
    return copy(
        snapshot = snapshot,
        screen = screen,
        playGame = game,
        playThing = thing,
    )
}
