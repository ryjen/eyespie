package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import kotlinx.coroutines.CoroutineScope

class PlayGameFactory(
    private val loader: PlayGameLoader,
    private val guessSubmitter: GuessSubmitter,
    private val output: (PlayGameOutput) -> Unit,
) {
    fun create(
        scope: CoroutineScope,
        gameId: GameId,
        thingId: ThingId,
    ): PlayGameInteractor = PlayGameInteractor(
        loader = loader,
        guessSubmitter = guessSubmitter,
        scope = scope,
        output = output,
        initialState = PlayGameState(gameId = gameId, thingId = thingId),
    )
}
