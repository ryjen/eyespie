package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.GuessOutcome

data class PlayGameContent(
    val gameName: String,
    val clueText: String,
    val matched: Boolean,
    val bestSimilarity: Double?,
    val clueNumber: Int = 1,
    val clueCount: Int = 1,
    val matchedClueCount: Int = if (matched) 1 else 0,
    val nextThingId: ThingId? = null,
)

data class PlayGameState(
    val gameId: GameId,
    val thingId: ThingId,
    val content: PlayGameContent? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val failure: PlayGameFailure? = null,
    val latestOutcome: GuessOutcome? = null,
    val guessGeneration: Long = 0,
) {
    val matched: Boolean
        get() = latestOutcome?.progress?.matched ?: content?.matched ?: false

    val matchedClues: Int
        get() {
            val content = content ?: return 0
            return content.matchedClueCount + if (!content.matched && matched) 1 else 0
        }

    val completed: Boolean
        get() = matched && content?.nextThingId == null && (content?.clueCount ?: 0) > 0
}
