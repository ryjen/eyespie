package com.micrantha.eyespie.features.create

import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.BaseInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CreateGameInteractor(
    private val port: CreateGamePort,
    private val scope: CoroutineScope,
    private val output: (CreateGameOutput) -> Unit,
    initialState: CreateGameState = CreateGameState(),
) : BaseInteractor<CreateGameState, CreateGameIntent>(initialState, CreateGameReducer) {
    override fun afterReduce(
        intent: CreateGameIntent,
        previousState: CreateGameState,
        stateAfterReduce: CreateGameState,
    ) {
        when (intent) {
            is CreateGameIntent.TargetCaptured -> scope.launch {
                when (
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
            }
            CreateGameIntent.Back -> output(CreateGameOutput.Cancelled)
            else -> Unit
        }
    }
}
