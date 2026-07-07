package com.micrantha.eyespie.features.game.data

import com.micrantha.eyespie.domain.entities.Game
import com.micrantha.eyespie.features.game.data.mapping.GameDomainMapper
import com.micrantha.eyespie.features.game.data.source.GameRemoteSource
import com.micrantha.eyespie.features.game.data.source.GamesLocalSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.micrantha.eyespie.domain.repository.GameRepository as DomainRepository

internal class GameDataRepository(
    private val remoteSource: GameRemoteSource,
    private val localSource: GamesLocalSource,
    private val mapper: GameDomainMapper
) : DomainRepository {

    override fun games(): Flow<Result<List<Game.Listing>>> = flow {
        val cached = localSource.getAll().map { it.map(mapper::list) }
        emit(cached)

        remoteSource.games()
            .onSuccess { nodes ->
                localSource.saveAll(nodes.map(mapper::data))
                emit(Result.success(nodes.map(mapper::list)))
            }
            .onFailure {
                if (cached.isFailure) emit(Result.failure(it))
            }
    }

    override fun game(id: String): Flow<Result<Game>> = flow {
        val cached = localSource.getAll().mapCatching { it.first { it.id == id } }.map(mapper::map)
        if (cached.isSuccess) {
            emit(cached)
        }

        remoteSource.game(id).map(mapper::map)
            .onSuccess { emit(Result.success(it)) }
            .onFailure {
                if (cached.isFailure) emit(Result.failure(it))
            }
    }
}
