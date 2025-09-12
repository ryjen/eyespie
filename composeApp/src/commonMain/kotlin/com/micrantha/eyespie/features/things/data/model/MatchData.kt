package com.micrantha.eyespie.features.things.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MatchRequest(
    @SerialName("query_embedding") val embedding: ByteArray,
    @SerialName("match_threshold") val threshold: Float,
    @SerialName("match_count") val count: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is MatchRequest) return false
        return embedding.contentEquals(other.embedding)
    }

    override fun hashCode() = embedding.contentHashCode()
}

@Serializable
data class MatchResponse(
    val id: String,
    val content: JsonElement,
    val similarity: Float
)
