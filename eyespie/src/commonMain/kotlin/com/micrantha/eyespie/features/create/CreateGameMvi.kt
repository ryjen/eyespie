package com.micrantha.eyespie.features.create

import com.micrantha.eyespie.features.app.AppFailure
import com.micrantha.eyespie.features.app.AppGameUseCases
import com.micrantha.eyespie.features.app.AppScreen
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.mvi.Interactor
import com.micrantha.eyespie.mvi.Reducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateGameState(
    val name: String = "",
    val clue: String = "",
    val expectedAnswer: String = "",
    val busy: Boolean = false,
    val failure: AppFailure? = null,
)

sealed interface CreateGameIntent {
    data class NameChanged(val value: String) : CreateGameIntent
    data class ClueChanged(val value: String) : CreateGameIntent
    data class ExpectedAnswerChanged(val value: String) : CreateGameIntent
    data class TargetCaptured(val image: CapturedImage) : CreateGameIntent
    data object CameraFailed : CreateGameIntent
    data object DismissFailure : CreateGameIntent
    data object Back : CreateGameIntent
    data object Created : CreateGameIntent
    data class OperationFailed(val failure: LocalGameFailure) : CreateGameIntent
}

object CreateGameReducer : Reducer<CreateGameState, CreateGameIntent> {
    override fun reduce(state: CreateGameState, intent: CreateGameIntent): CreateGameState = when (intent) {
        is CreateGameIntent.NameChanged -> state.copy(name = intent.value)
        is CreateGameIntent.ClueChanged -> state.copy(clue = intent.value)
        is CreateGameIntent.ExpectedAnswerChanged -> state.copy(expectedAnswer = intent.value)
        is CreateGameIntent.TargetCaptured -> state.copy(busy = true, failure = null)
        CreateGameIntent.CameraFailed -> state.copy(failure = AppFailure.CameraUnavailable)
        CreateGameIntent.DismissFailure -> state.copy(failure = null)
        CreateGameIntent.Back,
        CreateGameIntent.Created -> CreateGameState()
        is CreateGameIntent.OperationFailed -> state.copy(
            busy = false,
            failure = AppFailure.Game(intent.failure),
        )
    }
}

class CreateGameInteractor(
    private val useCases: AppGameUseCases,
    private val scope: CoroutineScope,
    private val navigate: (AppScreen) -> Unit,
    private val adoptSnapshot: (LocalGameSnapshot) -> Unit,
    private val reportHomeFailure: (LocalGameFailure) -> Unit,
    initialState: CreateGameState = CreateGameState(),
) : Interactor<CreateGameState, CreateGameIntent> {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<CreateGameState> = mutableState.asStateFlow()

    override fun dispatch(intent: CreateGameIntent) {
        mutableState.value = CreateGameReducer.reduce(mutableState.value, intent)
        val stateAfterReduce = mutableState.value
        when (intent) {
            is CreateGameIntent.TargetCaptured -> scope.launch {
                when (
                    val result = useCases.createGame(
                        name = stateAfterReduce.name,
                        clueText = stateAfterReduce.clue,
                        expectedAnswer = stateAfterReduce.expectedAnswer,
                        targetImage = intent.image,
                    )
                ) {
                    is LocalGameResult.Success -> when (val snapshot = useCases.loadSnapshot()) {
                        is LocalGameResult.Success -> {
                            adoptSnapshot(snapshot.value)
                            dispatch(CreateGameIntent.Created)
                            navigate(AppScreen.Home)
                        }
                        is LocalGameResult.Failure -> {
                            dispatch(CreateGameIntent.Created)
                            reportHomeFailure(snapshot.failure)
                            navigate(AppScreen.Home)
                        }
                    }
                    is LocalGameResult.Failure -> dispatch(CreateGameIntent.OperationFailed(result.failure))
                }
            }
            CreateGameIntent.Back -> navigate(AppScreen.Home)
            else -> Unit
        }
    }
}
