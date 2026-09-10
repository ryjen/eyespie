package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId

interface GameSaver {
    suspend fun save(gameId: GameId, gameName: String): GameDetailSaveResult
}

sealed interface GameDetailSaveResult {
    data object Saved : GameDetailSaveResult
    data object NotLocalCreator : GameDetailSaveResult
    data object TooLarge : GameDetailSaveResult
    data object Busy : GameDetailSaveResult
    data object Cancelled : GameDetailSaveResult
    data object Failed : GameDetailSaveResult
    data object Unavailable : GameDetailSaveResult
}
