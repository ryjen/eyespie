package com.micrantha.eyespie.features.clueauthoring

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.Interactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClueAuthoringInteractor(
    private val port: ClueAuthoringPort,
    private val scope: CoroutineScope,
    private val gameId: GameId,
    private val output: (ClueAuthoringOutput) -> Unit,
    initialState: ClueAuthoringState = ClueAuthoringState(),
) : Interactor<ClueAuthoringState, ClueAuthoringIntent> {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<ClueAuthoringState> = mutableState.asStateFlow()

    override fun dispatch(intent: ClueAuthoringIntent) {
        val previousState = mutableState.value
        mutableState.value = ClueAuthoringReducer.reduce(previousState, intent)
        when (intent) {
            is ClueAuthoringIntent.TargetCaptured -> if (!previousState.busy) {
                scope.launch {
                    when (
                        val result = port.addClue(
                            gameId = gameId,
                            clueText = previousState.clue,
                            expectedAnswer = previousState.expectedAnswer,
                            targetImage = intent.image,
                        )
                    ) {
                        is LocalGameResult.Success -> {
                            dispatch(ClueAuthoringIntent.Added)
                            output(ClueAuthoringOutput.Completed(gameId))
                        }
                        is LocalGameResult.Failure -> dispatch(ClueAuthoringIntent.OperationFailed(result.failure))
                    }
                }
            }
            ClueAuthoringIntent.Back -> output(ClueAuthoringOutput.Closed(gameId))
            else -> Unit
        }
    }
}
