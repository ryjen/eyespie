package com.micrantha.eyespie.features.clueauthoring

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.BaseInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ClueAuthoringInteractor(
    private val author: ClueAuthor,
    private val scope: CoroutineScope,
    private val gameId: GameId,
    private val output: (ClueAuthoringOutput) -> Unit,
    initialState: ClueAuthoringState = ClueAuthoringState(),
) : BaseInteractor<ClueAuthoringState, ClueAuthoringIntent>(initialState, ClueAuthoringReducer) {
    override fun afterReduce(
        intent: ClueAuthoringIntent,
        previousState: ClueAuthoringState,
        stateAfterReduce: ClueAuthoringState,
    ) {
        when (intent) {
            is ClueAuthoringIntent.TargetCaptured -> {
                scope.launch {
                    when (
                        val result = author.addClue(
                            gameId = gameId,
                            clueText = stateAfterReduce.clue,
                            expectedAnswer = stateAfterReduce.expectedAnswer,
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
