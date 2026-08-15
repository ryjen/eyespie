package com.micrantha.eyespie.features.things.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchRequest(
    @SerialName("target_thing_id") val thingID: String,
    @SerialName("query_embedding") val embedding: List<Float>,
    @SerialName("match_threshold") val threshold: Float,
)

@Serializable
data class MatchResponse(
    val id: String,
    val similarity: Float,
    val matched: Boolean,
)
