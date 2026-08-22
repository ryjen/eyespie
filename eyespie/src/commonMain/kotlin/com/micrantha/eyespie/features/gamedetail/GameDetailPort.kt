package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.LocalGameResult

interface GameDetailPort {
    suspend fun load(gameId: GameId): LocalGameResult<GameDetailContent>
}
