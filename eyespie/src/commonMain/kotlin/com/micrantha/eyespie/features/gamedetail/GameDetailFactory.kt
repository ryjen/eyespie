package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.GameSnapshotLoader
import kotlinx.coroutines.CoroutineScope

class GameDetailFactory(
    private val snapshotLoader: GameSnapshotLoader,
    private val sharer: GameSharer,
    private val output: (GameDetailOutput) -> Unit,
) {
    fun create(
        scope: CoroutineScope,
        gameId: GameId,
        initialState: GameDetailState = GameDetailState(),
    ): GameDetailInteractor = GameDetailInteractor(
        snapshotLoader = snapshotLoader,
        sharer = sharer,
        scope = scope,
        gameId = gameId,
        output = output,
        initialState = initialState,
    )
}
