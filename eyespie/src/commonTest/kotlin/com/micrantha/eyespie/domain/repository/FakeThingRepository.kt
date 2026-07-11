package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.entities.ThingList
import com.micrantha.eyespie.domain.entities.ThingMatches
import com.micrantha.eyespie.features.players.domain.entities.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

    override fun thing(thingID: String): Flow<Result<Thing>> =
        flowOf(things.find { it.id == thingID }?.let { Result.success(it) } ?: Result.failure(Exception("Not found")))

    override fun things(playerID: String): Flow<Result<ThingList>> =
        flowOf(Result.success(things.filter { it.createdBy.id == playerID }.map {
             Thing.Listing(it.id, it.id, it.createdAt, it.guessed, it.imageUrl)
        }))

    override fun nearby(location: Location.Point, distance: Double): Flow<Result<ThingList>> =
        flowOf(Result.success(things.map { Thing.Listing(it.id, it.id, it.createdAt, it.guessed, it.imageUrl) }))

    override fun match(embedding: Embedding): Flow<Result<ThingMatches>> =
        flowOf(matchResult ?: Result.success(emptyList()))
}
