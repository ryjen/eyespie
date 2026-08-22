package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.GuessOutcome

data class PlayGameContent(
    val gameName: String,
    val clueText: String,
    val matched: Boolean,
    val bestSimilarity: Double?,
)

data class PlayGameState(
    val gameId: GameId,
    val thingId: ThingId,
    val content: PlayGameContent? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val failure: PlayGameFailure? = null,
    val latestOutcome: GuessOutcome? = null,
)
