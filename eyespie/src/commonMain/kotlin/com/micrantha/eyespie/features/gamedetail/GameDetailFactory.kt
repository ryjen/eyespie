package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.GameThumbnailCache
import kotlinx.coroutines.CoroutineScope

class GameDetailFactory(
    private val snapshotLoader: GameSnapshotLoader,
    private val sharer: GameSharer,
    private val thumbnailCache: GameThumbnailCache,
    private val output: (GameDetailOutput) -> Unit,
    private val saver: GameSaver = UnavailableGameSaver,
) {
    fun create(
        scope: CoroutineScope,
        gameId: GameId,
        initialState: GameDetailState = GameDetailState(),
    ): GameDetailInteractor = GameDetailInteractor(
        snapshotLoader = snapshotLoader,
        sharer = sharer,
        saver = saver,
        thumbnailCache = thumbnailCache,
        scope = scope,
        gameId = gameId,
        output = output,
        initialState = initialState,
    )
}
