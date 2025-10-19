package com.micrantha.eyespie.features.things.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThingData(
    val id: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val name: String? = null,
    val imageUrl: String,
    val guessed: Boolean? = null,
    @SerialName("created_by") val createdBy: String,
    val location: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ThingData) return false
        return id == other.id
    }

    override fun hashCode() = id.hashCode()
}

typealias ThingRequest = ThingData
typealias ThingResponse = ThingData
typealias ThingListing = ThingData
