package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.Interactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameDetailInteractor(
    private val port: GameDetailPort,
    private val scope: CoroutineScope,
    private val gameId: GameId,
    private val output: (GameDetailOutput) -> Unit,
    initialState: GameDetailState = GameDetailState(),
) : Interactor<GameDetailState, GameDetailIntent> {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<GameDetailState> = mutableState.asStateFlow()

    override fun dispatch(intent: GameDetailIntent) {
        mutableState.value = GameDetailReducer.reduce(mutableState.value, intent)
        when (intent) {
            GameDetailIntent.Load -> {
                val generation = mutableState.value.loadGeneration
                scope.launch {
                    when (val result = port.load(gameId)) {
                        is LocalGameResult.Success -> dispatch(GameDetailIntent.ContentLoaded(generation, result.value))
                        is LocalGameResult.Failure -> dispatch(GameDetailIntent.OperationFailed(generation, result.failure))
                    }
                }
            }
            GameDetailIntent.Back -> output(GameDetailOutput.Closed)
            is GameDetailIntent.PlaySelected -> output(GameDetailOutput.PlayRequested(gameId, intent.thingId))
            else -> Unit
        }
    }
}
