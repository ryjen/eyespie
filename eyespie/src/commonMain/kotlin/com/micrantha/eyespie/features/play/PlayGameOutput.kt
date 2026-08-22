package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId

sealed interface PlayGameOutput {
    data class Closed(val gameId: GameId) : PlayGameOutput
    data class Advance(val gameId: GameId, val thingId: ThingId) : PlayGameOutput
}
