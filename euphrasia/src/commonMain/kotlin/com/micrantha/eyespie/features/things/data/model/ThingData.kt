package com.micrantha.eyespie.features.things.data.model

import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.graphql.type.GameThing
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class ThingData(
    val id: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("created_by") val createdBy: String,
    val location: String? = null,
    val proof: JsonElement? = null,
    @SerialName("game_thing") val game: ThingData.GameThing? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ThingData) return false
        return id == other.id
    }

    override fun hashCode() = id.hashCode()

    @Serializable
    data class GameThing(
        @SerialName("game_id") val gameId: String? = null,
        val guessed: Boolean? = null,
        @SerialName("created_at") val createdAt: String? = null
    )
}

typealias ThingRequest = ThingData
typealias ThingResponse = ThingData
typealias ThingListing = ThingData
