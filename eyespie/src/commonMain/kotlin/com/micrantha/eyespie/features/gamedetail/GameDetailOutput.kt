package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId

sealed interface GameDetailOutput {
    data object Closed : GameDetailOutput
    data class PlayRequested(val gameId: GameId, val thingId: ThingId) : GameDetailOutput
}
