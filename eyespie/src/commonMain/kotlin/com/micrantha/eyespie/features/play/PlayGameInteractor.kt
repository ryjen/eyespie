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
        if (!accepts(previousState, intent)) return

        mutableState.value = PlayGameReducer.reduce(previousState, intent)
        val stateAfterReduce = mutableState.value
        when (intent) {
            PlayGameIntent.Load -> {
                val generation = stateAfterReduce.loadGeneration
                scope.launch {
                    when (val result = port.load(stateAfterReduce.gameId, stateAfterReduce.thingId)) {
                        is LocalGameResult.Success -> dispatch(PlayGameIntent.ContentLoaded(generation, result.value))
                        is LocalGameResult.Failure -> dispatch(PlayGameIntent.LoadFailed(generation, result.failure))
                    }
                }
            }
            is PlayGameIntent.GuessCaptured -> {
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
            PlayGameIntent.Continue -> {
                val nextThingId = nextThingId(previousState)
                if (nextThingId != null) {
                    output(PlayGameOutput.NextRequested(previousState.gameId, nextThingId))
                } else {
                    output(PlayGameOutput.Closed(previousState.gameId))
                }
            }
            PlayGameIntent.Back -> output(PlayGameOutput.Closed(stateAfterReduce.gameId))
            else -> Unit
        }
    }

    private fun accepts(state: PlayGameState, intent: PlayGameIntent): Boolean = when (intent) {
        PlayGameIntent.Load -> !state.busy
        is PlayGameIntent.GuessCaptured ->
            !state.loading && !state.busy && state.content != null && !currentMatched(state)
        PlayGameIntent.Continue -> currentMatched(state)
        else -> true
    }

    private fun currentMatched(state: PlayGameState): Boolean =
        state.feedback is PlayFeedback.Matched || state.content?.matched == true

    private fun nextThingId(state: PlayGameState) =
        (state.feedback as? PlayFeedback.Matched)?.nextThingId ?: state.content?.nextThingId
}
