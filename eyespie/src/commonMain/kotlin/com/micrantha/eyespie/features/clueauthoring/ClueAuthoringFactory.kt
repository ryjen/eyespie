package com.micrantha.eyespie.features.clueauthoring

import com.micrantha.eyespie.core.GameId
import kotlinx.coroutines.CoroutineScope

class ClueAuthoringFactory(
    private val author: ClueAuthor,
    private val output: (ClueAuthoringOutput) -> Unit,
) {
    fun create(
        scope: CoroutineScope,
        gameId: GameId,
        initialState: ClueAuthoringState = ClueAuthoringState(),
    ): ClueAuthoringInteractor = ClueAuthoringInteractor(
        author = author,
        scope = scope,
        gameId = gameId,
        output = output,
        initialState = initialState,
    )
}
