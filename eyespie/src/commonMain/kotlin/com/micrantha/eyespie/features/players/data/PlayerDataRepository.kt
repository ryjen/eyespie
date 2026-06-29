package com.micrantha.eyespie.features.players.data

import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.features.players.data.mapping.PlayerDomainMapper
import com.micrantha.eyespie.features.players.data.source.PlayerRemoteSource
import com.micrantha.eyespie.features.players.data.source.PlayersLocalSource
import com.micrantha.eyespie.features.players.domain.repository.PlayerRepository

internal class PlayerDataRepository(
    private val remoteSource: PlayerRemoteSource,
    private val localSource: PlayersLocalSource,
    private val mapper: PlayerDomainMapper
) : PlayerRepository {

    override suspend fun players() = remoteSource.players()
        .onSuccess { localSource.saveAll(it) }
        .recover { localSource.getAll().getOrThrow() }
        .map { it.map(mapper::list) }

    override suspend fun nearby(location: Location.Point) = remoteSource.nearby(location)
        .onSuccess { localSource.saveAll(it) }
        .recover { localSource.getAll().getOrThrow() }
        .map { it.map(mapper::list) }

    override suspend fun player(userId: String) = remoteSource.player(userId)
        .recover {
            localSource.getAll().getOrThrow().first { it.user_id == userId }
        }
        .map(mapper::map)

    override suspend fun create(
        userId: String,
        firstName: String,
        lastName: String,
        nickName: String
    ) = remoteSource.create(userId, firstName, lastName, nickName).mapCatching {
        remoteSource.player(userId).getOrThrow()
    }.onSuccess {
        // We don't have a list of all players here, but we can't easily append to the file with current local source
        // For now, next refresh will update it.
    }.map(mapper::map)
}
