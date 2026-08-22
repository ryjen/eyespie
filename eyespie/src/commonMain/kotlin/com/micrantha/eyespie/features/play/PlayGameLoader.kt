package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.LocalGameResult

interface PlayGameLoader {
    suspend fun load(gameId: GameId, thingId: ThingId): LocalGameResult<PlayGameContent>
}
