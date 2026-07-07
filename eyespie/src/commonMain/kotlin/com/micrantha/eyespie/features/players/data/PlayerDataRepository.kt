package com.micrantha.eyespie.features.players.data

import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.features.players.data.mapping.PlayerDomainMapper
import com.micrantha.eyespie.features.players.data.source.PlayerRemoteSource
import com.micrantha.eyespie.features.players.data.source.PlayersLocalSource
import com.micrantha.eyespie.features.players.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class PlayerDataRepository(
    private val remoteSource: PlayerRemoteSource,
    private val localSource: PlayersLocalSource,
    private val mapper: PlayerDomainMapper
) : PlayerRepository {

    override fun players(): Flow<Result<List<com.micrantha.eyespie.features.players.domain.entities.Player.Listing>>> = flow {
        val cached = localSource.getAll().map { it.map(mapper::list) }
        emit(cached)

        remoteSource.players().onSuccess {
            localSource.saveAll(it)
            emit(Result.success(it.map(mapper::list)))
        }.onFailure {
            if (cached.isFailure) emit(Result.failure(it))
        }
    }

    override fun nearby(location: Location.Point): Flow<Result<List<com.micrantha.eyespie.features.players.domain.entities.Player.Listing>>> = flow {
        val cached = localSource.getAll().map { it.map(mapper::list) }
        emit(cached)

        remoteSource.nearby(location).onSuccess {
            localSource.saveAll(it)
            emit(Result.success(it.map(mapper::list)))
        }.onFailure {
            if (cached.isFailure) emit(Result.failure(it))
        }
    }

    override fun player(userId: String): Flow<Result<com.micrantha.eyespie.features.players.domain.entities.Player>> = flow {
        val cached = localSource.getAll().mapCatching { it.first { it.user_id == userId } }.map(mapper::map)
        if (cached.isSuccess) {
            emit(cached)
        }

        remoteSource.player(userId).map(mapper::map).onSuccess {
            emit(Result.success(it))
        }.onFailure {
            if (cached.isFailure) emit(Result.failure(it))
        }
    }

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
