package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.LocalGameFailure

data class HomeContent(
    val identityDisplayName: String,
    val identityIdSuffix: String,
    val games: List<HomeGame>,
)

data class HomeGame(
    val id: GameId,
    val name: String,
    val things: List<HomeThing>,
)

data class HomeThing(
    val id: ThingId,
    val clueText: String,
    val matched: Boolean,
    val bestSimilarity: Double?,
)

data class HomeState(
    val content: HomeContent? = null,
    val loading: Boolean = true,
    val failure: LocalGameFailure? = null,
    val refreshGeneration: Long = 0,
)
