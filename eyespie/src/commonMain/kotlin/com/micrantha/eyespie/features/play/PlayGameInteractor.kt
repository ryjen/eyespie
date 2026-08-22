package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.Interactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayGameInteractor(
    private val port: PlayGamePort,
    private val scope: CoroutineScope,
    private val output: (PlayGameOutput) -> Unit,
    initialState: PlayGameState,
) : Interactor<PlayGameState, PlayGameIntent> {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<PlayGameState> = mutableState.asStateFlow()

    override fun dispatch(intent: PlayGameIntent) {
        val previousState = mutableState.value
        mutableState.value = PlayGameReducer.reduce(previousState, intent)
        val stateAfterReduce = mutableState.value
        when (intent) {
            PlayGameIntent.Load -> scope.launch {
                when (val result = port.load(stateAfterReduce.gameId, stateAfterReduce.thingId)) {
                    is LocalGameResult.Success -> dispatch(PlayGameIntent.ContentLoaded(result.value))
                    is LocalGameResult.Failure -> dispatch(PlayGameIntent.LoadFailed(result.failure))
                }
            }
            is PlayGameIntent.GuessCaptured -> if (!previousState.busy && !previousState.matched && stateAfterReduce.busy) {
                val generation = stateAfterReduce.guessGeneration
                scope.launch {
                    when (
                        val result = port.guess(
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
