package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.LocalGameFailure

data class GameDetailContent(
    val name: String,
    val things: List<GameDetailThing>,
)

data class GameDetailThing(
    val id: ThingId,
    val clueText: String,
    val matched: Boolean,
    val bestSimilarity: Double?,
)

data class GameDetailState(
    val content: GameDetailContent? = null,
    val loading: Boolean = true,
    val failure: LocalGameFailure? = null,
    val loadGeneration: Long = 0,
)
