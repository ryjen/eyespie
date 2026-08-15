package com.micrantha.eyespie.features.things.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Full Thing authority. This DTO is creator-only under #122 RLS. */
@Serializable
data class ThingAuthorityData(
    val id: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("created_by") val createdBy: String,
    val location: String? = null,
    val proof: JsonElement? = null,
    val embedding: String? = null,
    val guessed: Boolean? = null,
)

/** Insert/update authority contract. Signed URLs are never accepted here. */
@Serializable
data class ThingRequest(
    val id: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("image_path") val imagePath: String,
    @SerialName("created_by") val createdBy: String,
    val location: String? = null,
    val proof: JsonElement? = null,
    val embedding: String? = null,
)

/** Safe list/nearby projection. No image path, location, proof, or embedding. */
@Serializable
data class ThingListing(
    val id: String,
    @SerialName("created_at") val createdAt: String? = null,
    val guessed: Boolean? = null,
)

typealias ThingResponse = ThingAuthorityData
