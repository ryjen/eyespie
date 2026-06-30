package com.micrantha.eyespie.features.things.data

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Location.Point
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.features.things.data.mapping.ThingsDomainMapper
import com.micrantha.eyespie.features.things.data.source.ThingsLocalSource
import com.micrantha.eyespie.features.things.data.source.ThingsRemoteSource
import com.micrantha.eyespie.domain.repository.ThingRepository as DomainRepository

internal class ThingDataRepository(
    private val remoteSource: ThingsRemoteSource,
    private val localSource: ThingsLocalSource,
    private val mapper: ThingsDomainMapper
) : DomainRepository {

    override suspend fun things(playerID: String) = remoteSource.things(playerID)
        .onSuccess { localSource.saveAll(it) }
        .recover { localSource.getAll().getOrThrow() }
        .map { it.map(mapper::list) }

    override suspend fun thing(thingID: String) = remoteSource.thing(thingID)
        .recover {
            localSource.getAll().getOrThrow().first { it.id == thingID }
        }
        .map(mapper::map)

    override suspend fun create(
        proof: Proof,
        imageUrl: String,
        playerID: String,
    ): Result<Thing> =
        remoteSource.save(mapper.new(proof, imageUrl, playerID)).map(mapper::map)

    override suspend fun nearby(
        location: Point,
        distance: Double
    ) = remoteSource.nearby(mapper.nearby(location, distance))
        .onSuccess { localSource.saveAll(it) }
        .recover { localSource.getAll().getOrThrow() }
        .map { it.map(mapper::list) }

    override suspend fun match(embedding: Embedding) = remoteSource
        .match(mapper.match(embedding)).map {
            it.map(mapper::match)
        }
}
