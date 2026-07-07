package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.entities.ThingList
import com.micrantha.eyespie.domain.entities.ThingMatches
import kotlinx.coroutines.flow.Flow

interface ThingRepository {

    fun things(playerID: String): Flow<Result<ThingList>>

    fun thing(thingID: String): Flow<Result<Thing>>

    suspend fun match(embedding: Embedding): Result<ThingMatches>

    suspend fun create(
        proof: Proof,
        imageUrl: String,
        playerID: String,
    ): Result<Thing>

    fun nearby(
        location: Location.Point,
        distance: Double = 10.0
    ): Flow<Result<ThingList>>
}
