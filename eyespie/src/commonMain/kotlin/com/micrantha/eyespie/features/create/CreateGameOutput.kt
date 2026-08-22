package com.micrantha.eyespie.features.create

import com.micrantha.eyespie.core.GameId

sealed interface CreateGameOutput {
    data object Created : CreateGameOutput
    data class ClueAdded(val gameId: GameId) : CreateGameOutput
    data class Cancelled(val returnGameId: GameId? = null) : CreateGameOutput
}
