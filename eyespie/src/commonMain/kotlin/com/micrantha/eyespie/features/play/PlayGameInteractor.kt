package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameFailureCode
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.BaseInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PlayGameInteractor(
    private val snapshotLoader: GameSnapshotLoader,
    private val guessSubmitter: GuessSubmitter,
    private val scope: CoroutineScope,
    private val output: (PlayGameOutput) -> Unit,
    initialState: PlayGameState,
) : BaseInteractor<PlayGameState, PlayGameIntent>(initialState, PlayGameReducer) {
    override fun afterReduce(
        intent: PlayGameIntent,
        previousState: PlayGameState,
        stateAfterReduce: PlayGameState,
    ) {
        when (intent) {
            PlayGameIntent.Load -> scope.launch {
                when (val result = snapshotLoader.loadSnapshot()) {
                    is LocalGameResult.Failure -> dispatch(PlayGameIntent.LoadFailed(result.failure))
                    is LocalGameResult.Success -> {
                        val content = PlayGameMapper.map(
                            result.value,
                            stateAfterReduce.gameId,
                            stateAfterReduce.thingId,
                        )
                        if (content == null) {
                            val gameExists = result.value.games.any { it.id == stateAfterReduce.gameId }
                            dispatch(
                                PlayGameIntent.LoadFailed(
                                    LocalGameFailure(
                                        if (gameExists) LocalGameFailureCode.THING_NOT_FOUND
                                        else LocalGameFailureCode.GAME_NOT_FOUND,
                                    ),
                                ),
                            )
                        } else {
                            dispatch(PlayGameIntent.ContentLoaded(content))
                        }
                    }
                }
            }
            is PlayGameIntent.GuessCaptured -> if (!previousState.busy && !previousState.matched && stateAfterReduce.busy) {
                val generation = stateAfterReduce.guessGeneration
                scope.launch {
                    when (
                        val result = guessSubmitter.guess(
                            gameId = stateAfterReduce.gameId,
                            thingId = stateAfterReduce.thingId,
                            guessImage = intent.image,
                        )
                    ) {
                        is LocalGameResult.Success -> dispatch(PlayGameIntent.GuessCompleted(generation, result.value))
                        is LocalGameResult.Failure -> dispatch(PlayGameIntent.OperationFailed(generation, result.failure))
                    }
                }
            }
            PlayGameIntent.NextClueSelected -> {
                if (previousState.matched) {
                    previousState.content?.nextThingId?.let { nextThingId ->
                        output(PlayGameOutput.Advance(previousState.gameId, nextThingId))
                    }
                }
            }
            PlayGameIntent.Back -> output(PlayGameOutput.Closed(previousState.gameId))
            else -> Unit
        }
    }
}
