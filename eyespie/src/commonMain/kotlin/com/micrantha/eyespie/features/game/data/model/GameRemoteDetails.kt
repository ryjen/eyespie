package com.micrantha.eyespie.features.game.data.model

import com.micrantha.eyespie.graphql.GameNodeQuery
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal data class GameRemoteDetails(
    val node: GameNodeQuery.GameNode,
    val things: List<SafeGameThingData>,
)

@Serializable
internal data class SafeGameThingData(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    val guessed: Boolean = false,
)
