package com.micrantha.eyespie.features.clueauthoring

import com.micrantha.eyespie.core.GameId

sealed interface ClueAuthoringOutput {
    data class Closed(val gameId: GameId) : ClueAuthoringOutput
    data class Completed(val gameId: GameId) : ClueAuthoringOutput
}
