package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.LocalGameResult

sealed interface GameDetailShareResult {
    data object Shared : GameDetailShareResult
    data object NotLocalCreator : GameDetailShareResult
    data object TooLarge : GameDetailShareResult
    data object Busy : GameDetailShareResult
    data object Cancelled : GameDetailShareResult
    data object Failed : GameDetailShareResult
    data object Unavailable : GameDetailShareResult
}

interface GameDetailPort {
    suspend fun load(gameId: GameId): LocalGameResult<GameDetailContent>

    suspend fun share(gameId: GameId, gameName: String): GameDetailShareResult =
        GameDetailShareResult.Unavailable
}
