package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.entities.ThingList
import com.micrantha.eyespie.domain.entities.ThingMatches
import com.micrantha.eyespie.features.players.domain.entities.Player
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class FakeThingRepository : ThingRepository {
    val things = mutableListOf<Thing>()
    var createResult: Result<Thing>? = null
    var matchResult: Result<ThingMatches>? = null

    override suspend fun create(proof: Proof, imageUrl: String, playerID: String): Result<Thing> {
        return createResult ?: run {
            val thing = Thing(
                id = "t${things.size + 1}",
                createdBy = Player.Ref(playerID, "player"),
                imageUrl = imageUrl,
                createdAt = System.now(),
                location = Location.Point(0.0, 0.0),
                guessed = false,
                guesses = emptyList()
            )
            things.add(thing)
            Result.success(thing)
        }
    }

    override suspend fun thing(thingID: String): Result<Thing> =
        things.find { it.id == thingID }?.let { Result.success(it) } ?: Result.failure(Exception("Not found"))

    override suspend fun things(playerID: String): Result<ThingList> =
        Result.success(things.filter { it.createdBy.id == playerID }.map {
             Thing.Listing(it.id, it.id, it.createdAt, it.guessed, it.imageUrl)
        })

    override suspend fun nearby(location: Location.Point, distance: Double): Result<ThingList> =
        Result.success(things.map { Thing.Listing(it.id, it.id, it.createdAt, it.guessed, it.imageUrl) })

    override suspend fun match(embedding: Embedding): Result<ThingMatches> =
        matchResult ?: Result.success(emptyList())
}
