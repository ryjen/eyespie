package com.micrantha.eyespie.features.things.data

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Location.Point
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.entities.requireCanonical
import com.micrantha.eyespie.features.things.data.mapping.ThingsDomainMapper
import com.micrantha.eyespie.features.things.data.source.ThingsLocalSource
import com.micrantha.eyespie.features.things.data.source.ThingsRemoteSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.micrantha.eyespie.domain.repository.ThingRepository as DomainRepository

internal class ThingDataRepository(
    private val remoteSource: ThingsRemoteSource,
    private val localSource: ThingsLocalSource,
    private val mapper: ThingsDomainMapper
) : DomainRepository {

    override fun things(playerID: String): Flow<Result<List<Thing.Listing>>> = flow {
        remoteSource.things(playerID)
            .mapCatching { things -> things.map(mapper::list) }
            .also { emit(it) }
    }

    /** Full Thing authority is owner-only. Safe listing paths never hydrate this DTO/cache. */
    override fun thing(thingID: String): Flow<Result<Thing>> = flow {
        val cached = localSource.getAll().mapCatching { things ->
            mapper.map(things.first { it.id == thingID })
        }
        if (cached.isSuccess) {
            emit(cached)
        }

        remoteSource.thing(thingID).mapCatching(mapper::map).onSuccess {
            emit(Result.success(it))
        }.onFailure {
            if (cached.isFailure) emit(Result.failure(it))
        }
    }

    override suspend fun create(
        proof: Proof,
        imageUrl: String,
        playerID: String,
    ): Result<Thing> {
        val request = runCatching { mapper.new(proof, imageUrl, playerID) }
            .getOrElse { return Result.failure(it) }
        return remoteSource.save(request).mapCatching(mapper::map)
    }

    override fun nearby(
        location: Point,
        distance: Double
    ): Flow<Result<List<Thing.Listing>>> = flow {
        remoteSource.nearby(mapper.nearby(location, distance))
            .mapCatching { things -> things.map(mapper::list) }
            .also { emit(it) }
    }

    override fun match(thingID: String, embedding: Embedding): Flow<Result<Thing.Match>> = flow {
        val canonical = runCatching { embedding.requireCanonical() }
            .getOrElse {
                emit(Result.failure(it))
                return@flow
            }

        remoteSource.match(mapper.match(thingID, canonical))
            .mapCatching(mapper::match)
            .onSuccess { emit(Result.success(it)) }
            .onFailure { emit(Result.failure(it)) }
    }
}
