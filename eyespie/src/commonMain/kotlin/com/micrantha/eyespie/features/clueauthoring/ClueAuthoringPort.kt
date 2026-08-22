package com.micrantha.eyespie.features.clueauthoring

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.AuthoredThing
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.imaging.CapturedImage

interface ClueAuthoringPort {
    suspend fun addClue(
        gameId: GameId,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<AuthoredThing>
}
