package com.micrantha.eyespie.features.create

import com.micrantha.eyespie.mvi.Reducer

object CreateGameReducer : Reducer<CreateGameState, CreateGameIntent> {
    override fun reduce(state: CreateGameState, intent: CreateGameIntent): CreateGameState = when (intent) {
        is CreateGameIntent.NameChanged -> state.copy(name = intent.value)
        is CreateGameIntent.ClueChanged -> state.copy(clue = intent.value)
        is CreateGameIntent.ExpectedAnswerChanged -> state.copy(expectedAnswer = intent.value)
        is CreateGameIntent.TargetCaptured -> state.copy(busy = true, failure = null)
        CreateGameIntent.CameraFailed -> state.copy(failure = CreateGameFailure.CameraUnavailable)
        CreateGameIntent.DismissFailure -> state.copy(failure = null)
        CreateGameIntent.Back,
        CreateGameIntent.Created -> CreateGameState()
        is CreateGameIntent.OperationFailed -> state.copy(
            busy = false,
            failure = CreateGameFailure.Game(intent.failure),
        )
    }
}
