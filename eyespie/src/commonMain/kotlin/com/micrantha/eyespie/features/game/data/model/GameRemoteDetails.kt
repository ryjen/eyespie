package com.micrantha.eyespie.features.game.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal data class GameRemoteDetails(
    val id: String,
    val name: String,
    val createdAt: String,
    val expiresAt: String,
    val turnDuration: String,
    val minThings: Int?,
    val maxThings: Int?,
    val minPlayers: Int?,
    val maxPlayers: Int?,
    val players: List<SafeGamePlayerData>,
    val things: List<SafeGameThingData>,
)

internal data class SafeGamePlayerData(
    val id: String,
    val nodeId: String,
    val createdAt: String,
    val firstName: String,
    val lastName: String,
    val score: Int,
)

@Serializable
internal data class SafeGameThingData(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    val guessed: Boolean = false,
)
