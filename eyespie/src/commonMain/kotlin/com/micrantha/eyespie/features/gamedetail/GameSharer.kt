package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId

interface GameSharer {
    suspend fun share(gameId: GameId, gameName: String): GameDetailShareResult
}
