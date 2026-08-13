package com.micrantha.eyespie.features.things.data

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Location.Point
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.entities.ThingMatches
import com.micrantha.eyespie.domain.entities.cosineSimilarity
import com.micrantha.eyespie.domain.entities.requireCanonical
import com.micrantha.eyespie.domain.entities.toPostgresEmbedding
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
        val cached = localSource.getAll().map { it.map(mapper::list) }
        emit(cached)

        remoteSource.things(playerID).onSuccess {
            localSource.saveAll(it)
            emit(Result.success(it.map(mapper::list)))
        }.onFailure {
            if (cached.isFailure) emit(Result.failure(it))
        }
    }

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
        val cached = localSource.getAll().map { it.map(mapper::list) }
        emit(cached)

        remoteSource.nearby(mapper.nearby(location, distance)).onSuccess {
            localSource.saveAll(it)
            emit(Result.success(it.map(mapper::list)))
        }.onFailure {
            if (cached.isFailure) emit(Result.failure(it))
        }
    }

    override fun match(embedding: Embedding): Flow<Result<ThingMatches>> = flow {
        val canonical = runCatching { embedding.requireCanonical() }
            .getOrElse {
                emit(Result.failure(it))
                return@flow
            }

        val localMatches = localSource.getAll().mapCatching { localThings ->
            localThings.mapNotNull { thing ->
                val vector = thing.embedding ?: return@mapNotNull null
                runCatching {
                    val thingEmbedding = vector.toPostgresEmbedding()
                    val similarity = canonical.cosineSimilarity(thingEmbedding)
                    if (similarity >= mapper.matchThreshold) {
                        Thing.Match(thing.id!!, thingEmbedding, similarity)
                    } else {
                        null
                    }
                }.getOrNull()
            }.sortedByDescending { it.similarity }.take(mapper.matchCount)
        }

        localMatches.onSuccess {
            emit(Result.success(it))
        }

        remoteSource.match(mapper.match(canonical)).mapCatching { matches ->
            matches.map(mapper::match)
        }.onSuccess {
            emit(Result.success(it))
        }.onFailure {
            if (localMatches.isFailure) emit(Result.failure(it))
        }
    }
}
