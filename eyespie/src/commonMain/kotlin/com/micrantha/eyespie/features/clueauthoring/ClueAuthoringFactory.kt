package com.micrantha.eyespie.features.clueauthoring

import com.micrantha.eyespie.core.GameId
import kotlinx.coroutines.CoroutineScope

class ClueAuthoringFactory(
    private val port: ClueAuthoringPort,
    private val output: (ClueAuthoringOutput) -> Unit,
) {
    fun create(
        scope: CoroutineScope,
        gameId: GameId,
        initialState: ClueAuthoringState = ClueAuthoringState(),
    ): ClueAuthoringInteractor = ClueAuthoringInteractor(
        port = port,
        scope = scope,
        gameId = gameId,
        output = output,
        initialState = initialState,
    )
}
