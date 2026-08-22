package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId
import kotlinx.coroutines.CoroutineScope

class GameDetailFactory(
    private val loader: GameDetailLoader,
    private val sharer: GameSharer,
    private val output: (GameDetailOutput) -> Unit,
) {
    fun create(
        scope: CoroutineScope,
        gameId: GameId,
        initialState: GameDetailState = GameDetailState(),
    ): GameDetailInteractor = GameDetailInteractor(
        loader = loader,
        sharer = sharer,
        scope = scope,
        gameId = gameId,
        output = output,
        initialState = initialState,
    )
}
