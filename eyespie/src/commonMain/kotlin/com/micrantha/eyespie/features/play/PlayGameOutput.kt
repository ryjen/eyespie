package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.core.GameId

sealed interface PlayGameOutput {
    data class Closed(val gameId: GameId) : PlayGameOutput
}
