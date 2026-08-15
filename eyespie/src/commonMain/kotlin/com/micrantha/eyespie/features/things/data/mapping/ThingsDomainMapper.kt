package com.micrantha.eyespie.features.things.data.mapping

import com.micrantha.eyespie.core.data.system.mapping.LocationDomainMapper
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Location.Point
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.entities.floats
import com.micrantha.eyespie.domain.entities.requireCanonical
import com.micrantha.eyespie.domain.entities.toPostgresEmbedding
import com.micrantha.eyespie.domain.entities.toPostgresVector
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.things.data.model.MatchRequest
import com.micrantha.eyespie.features.things.data.model.MatchResponse
import com.micrantha.eyespie.features.things.data.model.NearbyRequest
import com.micrantha.eyespie.features.things.data.model.ThingListing
import com.micrantha.eyespie.features.things.data.model.ThingRequest
import com.micrantha.eyespie.features.things.data.model.ThingResponse
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class ThingsDomainMapper(
    private val locationMapper: LocationDomainMapper,
    val matchThreshold: Float = 0.5f,
) {

    fun new(proof: Proof, imagePath: String, playerId: String) =
        ThingRequest(
            imagePath = imagePath,
            createdBy = playerId,
            location = proof.location.toString(),
            embedding = proof.embedding.toPostgresVector()
        )

    fun map(thing: Thing) = ThingRequest(
        id = thing.id,
        createdAt = thing.createdAt.toString(),
        imagePath = thing.imagePath,
        createdBy = thing.createdBy.id,
        location = thing.location.toString(),
        embedding = thing.embedding?.toPostgresVector()
    )

    fun map(data: ThingResponse): Thing {
        val point = data.location?.let { locationMapper.point(it) } ?: Point()
        val imagePath = data.imagePath
            ?: throw IllegalArgumentException("Thing authority is missing durable image path")

        return Thing(
            id = data.id!!,
            createdAt = data.createdAt?.let { Instant.parse(it) } ?: System.now(),
            imagePath = imagePath,
            guessed = data.guessed ?: false,
            createdBy = Player.Ref(
                id = data.createdBy,
                name = "" // TODO: graphql
            ),
            guesses = emptyList(),
            location = point,
            embedding = data.embedding?.toPostgresEmbedding()
        )
    }

    fun list(data: ThingListing) = Thing.Listing(
        id = data.id,
        createdAt = data.createdAt?.let { Instant.parse(it) } ?: System.now(),
        nodeId = data.id,
        guessed = data.guessed == true,
    )

    fun nearby(location: Point, distance: Double) = NearbyRequest(
        latitude = location.latitude,
        longitude = location.longitude,
        distance = distance
    )

    fun match(thingID: String, embedding: Embedding): MatchRequest = MatchRequest(
        thingID = thingID,
        embedding = embedding.requireCanonical().floats(),
        threshold = matchThreshold,
    )

    fun match(data: MatchResponse) = Thing.Match(
        id = data.id,
        similarity = data.similarity,
        matched = data.matched,
    )
}
