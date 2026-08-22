package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId
import kotlinx.coroutines.CoroutineScope

class GameDetailFactory(
    private val port: GameDetailPort,
    private val output: (GameDetailOutput) -> Unit,
) {
    fun create(
        scope: CoroutineScope,
        gameId: GameId,
        initialState: GameDetailState = GameDetailState(),
    ): GameDetailInteractor = GameDetailInteractor(
        port = port,
        scope = scope,
        gameId = gameId,
        output = output,
        initialState = initialState,
    )
}
