package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.entities.ThingList
import kotlinx.coroutines.flow.Flow

interface ThingRepository {

    fun things(playerID: String): Flow<Result<ThingList>>

    /** Creator-authority lookup. Game/guesser paths must not use this full-row contract. */
    fun thing(thingID: String): Flow<Result<Thing>>

    fun match(thingID: String, embedding: Embedding): Flow<Result<Thing.Match>>

    suspend fun create(
        proof: Proof,
        imagePath: String,
        playerID: String,
    ): Result<Thing>

    fun nearby(
        location: Location.Point,
        distance: Double = 10.0
    ): Flow<Result<ThingList>>
}
