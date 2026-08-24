package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameFailureCode
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.BaseInteractor
import com.micrantha.eyespie.mvi.EffectEmitter
import com.micrantha.eyespie.mvi.EffectSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class GameDetailInteractor(
    private val snapshotLoader: GameSnapshotLoader,
    private val sharer: GameSharer,
    private val scope: CoroutineScope,
    private val gameId: GameId,
    private val output: (GameDetailOutput) -> Unit,
    initialState: GameDetailState = GameDetailState(),
) : BaseInteractor<GameDetailState, GameDetailIntent>(initialState, GameDetailReducer), EffectSource<GameDetailEffect> {
    private val effectEmitter = EffectEmitter<GameDetailEffect>()
    override val effects: Flow<GameDetailEffect> = effectEmitter.effects

    override fun afterReduce(
        intent: GameDetailIntent,
        previousState: GameDetailState,
        stateAfterReduce: GameDetailState,
    ) {
        when (intent) {
            GameDetailIntent.Load -> {
                val generation = stateAfterReduce.loadGeneration
                scope.launch {
                    when (val result = snapshotLoader.loadSnapshot()) {
                        is LocalGameResult.Failure -> dispatch(
                            GameDetailIntent.OperationFailed(generation, result.failure),
                        )
                        is LocalGameResult.Success -> {
                            val content = GameDetailMapper.map(result.value, gameId)
                            if (content == null) {
                                dispatch(
                                    GameDetailIntent.OperationFailed(
                                        generation,
                                        LocalGameFailure(LocalGameFailureCode.GAME_NOT_FOUND),
                                    ),
                                )
                            } else {
                                dispatch(GameDetailIntent.ContentLoaded(generation, content))
                            }
                        }
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
            is GameDetailIntent.ShareFinished -> if (intent.result != GameDetailShareResult.Cancelled) {
                effectEmitter.emit(GameDetailEffect.ShareFinished(intent.result))
            }
            GameDetailIntent.Back -> output(GameDetailOutput.Closed)
            is GameDetailIntent.PlaySelected -> output(GameDetailOutput.PlayRequested(gameId, intent.thingId))
            else -> Unit
        }
    }
}
