package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.BaseInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class GameDetailInteractor(
    private val loader: GameDetailLoader,
    private val sharer: GameSharer,
    private val scope: CoroutineScope,
    private val gameId: GameId,
    private val output: (GameDetailOutput) -> Unit,
    initialState: GameDetailState = GameDetailState(),
) : BaseInteractor<GameDetailState, GameDetailIntent>(initialState, GameDetailReducer) {
    override fun afterReduce(
        intent: GameDetailIntent,
        previousState: GameDetailState,
        stateAfterReduce: GameDetailState,
    ) {
        when (intent) {
            GameDetailIntent.Load -> {
                val generation = stateAfterReduce.loadGeneration
                scope.launch {
                    when (val result = loader.load(gameId)) {
                        is LocalGameResult.Success -> dispatch(GameDetailIntent.ContentLoaded(generation, result.value))
                        is LocalGameResult.Failure -> dispatch(GameDetailIntent.OperationFailed(generation, result.failure))
                    }
                }
            }
            GameDetailIntent.AddClueSelected -> if (previousState.content?.localCreator == true) {
                output(GameDetailOutput.AddClueRequested(gameId))
            }
            GameDetailIntent.ShareSelected -> if (
                !previousState.shareInProgress && previousState.content?.localCreator == true
            ) {
                val gameName = previousState.content.name
                scope.launch {
                    dispatch(GameDetailIntent.ShareFinished(sharer.share(gameId, gameName)))
                }
            }
            GameDetailIntent.Back -> output(GameDetailOutput.Closed)
            is GameDetailIntent.PlaySelected -> output(GameDetailOutput.PlayRequested(gameId, intent.thingId))
            else -> Unit
        }
    }
}
