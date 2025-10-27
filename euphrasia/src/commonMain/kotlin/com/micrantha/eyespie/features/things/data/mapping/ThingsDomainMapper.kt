package com.micrantha.eyespie.features.things.data.mapping

import com.micrantha.eyespie.core.data.system.mapping.LocationDomainMapper
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Location.Point
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.things.data.model.MatchRequest
import com.micrantha.eyespie.features.things.data.model.MatchResponse
import com.micrantha.eyespie.features.things.data.model.NearbyRequest
import com.micrantha.eyespie.features.things.data.model.ThingListing
import com.micrantha.eyespie.features.things.data.model.ThingRequest
import com.micrantha.eyespie.features.things.data.model.ThingResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class ThingsDomainMapper(
    private val locationMapper: LocationDomainMapper,
) {

    fun new(proof: Proof, imageUrl: String, playerId: String) =
        ThingRequest(
            imageUrl = imageUrl,
            createdBy = playerId,
            location = proof.location.toString(),
        )

    fun map(thing: Thing) = ThingRequest(
        id = thing.id,
        createdAt = thing.createdAt.toString(),
        imageUrl = thing.imageUrl,
        createdBy = thing.createdBy.id,
        location = thing.location.toString(),
    )

    fun map(data: ThingResponse): Thing {
        val point = data.location?.let { locationMapper.point(it) } ?: Point()

        return Thing(
            id = data.id!!,
            createdAt = data.createdAt?.let { Instant.parse(it) } ?: System.now(),
            imageUrl = data.imageUrl,
            guessed = data.game?.guessed ?: false,
            createdBy = Player.Ref(
                id = data.createdBy,
                name = "" // TODO: graphql
            ),
            guesses = emptyList(),
            location = point,
        )
    }

    fun list(data: ThingListing) = Thing.Listing(
        id = data.id!!,
        createdAt = data.createdAt?.let { Instant.parse(it) } ?: System.now(),
        nodeId = data.id,
        guessed = data.game?.guessed == true,
        imageUrl = data.imageUrl
    )

    fun nearby(location: Point, distance: Double) = NearbyRequest(
        latitude = location.latitude,
        longitude = location.longitude,
        distance = distance
    )

    fun match(embedding: Embedding) = MatchRequest(
        embedding = embedding.toByteArray(),
        threshold = 0.5f,
        count = 5,
    )

    fun match(data: MatchResponse) = Thing.Match(
        id = data.id,
        embedding = Json.decodeFromJsonElement(data.content),
        similarity = data.similarity
    )
}
