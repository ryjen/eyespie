package com.micrantha.eyespie.features.create

import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.Interactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateGameInteractor(
    private val port: CreateGamePort,
    private val scope: CoroutineScope,
    private val output: (CreateGameOutput) -> Unit,
    initialState: CreateGameState = CreateGameState(),
) : Interactor<CreateGameState, CreateGameIntent> {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<CreateGameState> = mutableState.asStateFlow()

    override fun dispatch(intent: CreateGameIntent) {
        mutableState.value = CreateGameReducer.reduce(mutableState.value, intent)
        val stateAfterReduce = mutableState.value
        when (intent) {
            is CreateGameIntent.TargetCaptured -> scope.launch {
                when (val mode = stateAfterReduce.mode) {
                    CreateGameMode.NewGame -> when (
                        val result = port.create(
                            name = stateAfterReduce.name,
                            clueText = stateAfterReduce.clue,
                            expectedAnswer = stateAfterReduce.expectedAnswer,
                            targetImage = intent.image,
                        )
                    ) {
                        is LocalGameResult.Success -> {
                            dispatch(CreateGameIntent.Created)
                            output(CreateGameOutput.Created)
                        }
                        is LocalGameResult.Failure -> dispatch(CreateGameIntent.OperationFailed(result.failure))
                    }
                    is CreateGameMode.AddClue -> when (
                        val result = port.addClue(
                            gameId = mode.gameId,
                            clueText = stateAfterReduce.clue,
                            expectedAnswer = stateAfterReduce.expectedAnswer,
                            targetImage = intent.image,
                        )
                    ) {
                        is LocalGameResult.Success -> {
                            dispatch(CreateGameIntent.Created)
                            output(CreateGameOutput.ClueAdded(mode.gameId))
                        }
                        is LocalGameResult.Failure -> dispatch(CreateGameIntent.OperationFailed(result.failure))
                    }
                }
            }
            CreateGameIntent.Back -> {
                val returnGameId = (stateAfterReduce.mode as? CreateGameMode.AddClue)?.gameId
                output(CreateGameOutput.Cancelled(returnGameId))
            }
            else -> Unit
        }
    }
}
