package com.micrantha.eyespie.features.things.data

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Location.Point
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.entities.ThingMatches
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
        val cached = localSource.getAll().mapCatching { it.first { it.id == thingID } }.map(mapper::map)
        if (cached.isSuccess) {
            emit(cached)
        }

        remoteSource.thing(thingID).map(mapper::map).onSuccess {
            emit(Result.success(it))
        }.onFailure {
            if (cached.isFailure) emit(Result.failure(it))
        }
    }

    override suspend fun create(
        proof: Proof,
        imageUrl: String,
        playerID: String,
    ): Result<Thing> =
        runCatching { mapper.new(proof, imageUrl, playerID) }
            .mapCatching { remoteSource.save(it).getOrThrow() }
            .map(mapper::map)

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
        val localThings = localSource.getAll().getOrDefault(emptyList())
        val localMatches = localThings.mapNotNull { thing ->
            val thingEmbedding = mapper.decodeEmbedding(thing) ?: return@mapNotNull null
            runCatching {
                val similarity = embedding.cosineSimilarity(thingEmbedding)
                if (similarity >= mapper.matchThreshold) {
                    Thing.Match(thing.id!!, similarity)
                } else {
                    null
                }
            }.getOrNull()
        }.sortedByDescending { it.similarity }.take(mapper.matchCount)

        emit(Result.success(localMatches))

        remoteSource.match(mapper.match(embedding)).map {
            it.map(mapper::match)
        }.onSuccess {
            emit(Result.success(it))
        }
    }
}
