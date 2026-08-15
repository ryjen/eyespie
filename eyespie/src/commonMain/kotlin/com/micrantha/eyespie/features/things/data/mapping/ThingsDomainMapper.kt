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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
<<<<<<< Updated upstream
||||||| Stash base
import okio.ByteString.Companion.decodeHex
=======
import kotlinx.serialization.json.encodeToJsonElement
import com.micrantha.eyespie.domain.entities.AiProof
import okio.ByteString.Companion.decodeHex
>>>>>>> Stashed changes
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class ThingsDomainMapper(
    private val locationMapper: LocationDomainMapper,
    val matchThreshold: Float = 0.5f,
    val matchCount: Int = 5
) {

    fun new(proof: Proof, imageUrl: String, playerId: String) =
        ThingRequest(
            imageUrl = imageUrl,
            createdBy = playerId,
<<<<<<< Updated upstream
            location = proof.location.toString(),
            embedding = proof.embedding.toPostgresVector()
||||||| Stash base
            location = proof.location.toString(),
            embedding = proof.embedding.floats().joinToString(prefix = "[", postfix = "]", separator = ","),
            modelVersion = proof.modelVersion
=======
            location = proof.location?.point?.toString(),
            embedding = proof.embedding.floats().joinToString(prefix = "[", postfix = "]", separator = ","),
            modelVersion = proof.modelVersion,
            proof = proof.clues?.let { Json.encodeToJsonElement(it) }
>>>>>>> Stashed changes
        )

    fun map(thing: Thing) = ThingRequest(
        id = thing.id,
        createdAt = thing.createdAt.toString(),
        imageUrl = thing.imageUrl,
        createdBy = thing.createdBy.id,
        location = thing.location.toString(),
<<<<<<< Updated upstream
        embedding = thing.embedding?.toPostgresVector()
||||||| Stash base
        embedding = thing.embedding?.floats()?.joinToString(prefix = "[", postfix = "]", separator = ","),
        modelVersion = thing.modelVersion
=======
        embedding = thing.embedding?.floats()?.joinToString(prefix = "[", postfix = "]", separator = ","),
        modelVersion = thing.modelVersion,
        proof = Json.encodeToJsonElement(thing.clues)
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
            embedding = data.embedding?.toPostgresEmbedding()
||||||| Stash base
            embedding = data.embedding?.let { hex ->
                try {
                    hex.decodeHex()
                } catch (_: Throwable) {
                    null
                }
            },
            modelVersion = data.modelVersion
=======
            clues = data.proof?.let { Json.decodeFromJsonElement<AiProof>(it) } ?: emptySet(),
            embedding = data.embedding?.let { hex ->
                try {
                    hex.decodeHex()
                } catch (_: Throwable) {
                    null
                }
            },
            modelVersion = data.modelVersion
>>>>>>> Stashed changes
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

    fun match(embedding: Embedding): MatchRequest = MatchRequest(
        embedding = embedding.requireCanonical().floats(),
        threshold = matchThreshold,
        count = matchCount,
    )

    fun match(data: MatchResponse): Thing.Match {
        val content = Json.decodeFromJsonElement<ThingResponse>(data.content)
        if (content.id != null && content.id != data.id) {
            throw IllegalArgumentException("match RPC Thing id does not match result id")
        }
        val embedding = content.embedding?.toPostgresEmbedding()
            ?: throw IllegalArgumentException("match RPC Thing is missing embedding")

        return Thing.Match(
            id = data.id,
            embedding = embedding,
            similarity = data.similarity
        )
    }
}
