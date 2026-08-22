package com.micrantha.eyespie.features.create

import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.imaging.CapturedImage

interface GameCreator {
    suspend fun create(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedGame>
}
