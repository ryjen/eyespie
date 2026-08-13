package com.micrantha.eyespie.features.things.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchRequest(
    @SerialName("query_embedding") val embedding: List<Float>,
    @SerialName("query_embedding_model_id") val embeddingModelId: String,
    @SerialName("match_threshold") val threshold: Float,
    @SerialName("match_count") val count: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is MatchRequest) return false
        return embedding == other.embedding && embeddingModelId == other.embeddingModelId
    }

    override fun hashCode(): Int = 31 * embedding.hashCode() + embeddingModelId.hashCode()
}

@Serializable
data class MatchResponse(
    val id: String,
    val similarity: Float
)
