package com.micrantha.eyespie.features.create

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.CreatedClue
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.imaging.CapturedImage

interface CreateGamePort {
    suspend fun create(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedGame>

    suspend fun addClue(
        gameId: GameId,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedClue>
}
