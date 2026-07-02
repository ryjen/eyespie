package com.micrantha.eyespie.features.game.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GameData(
    val id: String,
    val title: String,
    val createdAt: String,
    val expiresAt: String?,
    val creatorId: String,
    val playerCount: Int
)
