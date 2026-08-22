package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.imaging.CapturedImage

interface PlayGamePort {
    suspend fun load(gameId: GameId, thingId: ThingId): LocalGameResult<PlayGameContent>

    suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): LocalGameResult<GuessOutcome>
}
