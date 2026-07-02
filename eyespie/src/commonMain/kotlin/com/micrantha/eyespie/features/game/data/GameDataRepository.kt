package com.micrantha.eyespie.features.game.data

import com.micrantha.eyespie.domain.entities.Game
import com.micrantha.eyespie.features.game.data.mapping.GameDomainMapper
import com.micrantha.eyespie.features.game.data.source.GameRemoteSource
import com.micrantha.eyespie.features.game.data.source.GamesLocalSource
import com.micrantha.eyespie.domain.repository.GameRepository as DomainRepository

internal class GameDataRepository(
    private val remoteSource: GameRemoteSource,
    private val localSource: GamesLocalSource,
    private val mapper: GameDomainMapper
) : DomainRepository {

    override suspend fun games(): Result<List<Game.Listing>> = remoteSource.games()
        .onSuccess { nodes -> localSource.saveAll(nodes.map(mapper::data)) }
        .map { nodes -> nodes.map(mapper::list) }
        .recover { localSource.getAll().getOrThrow().map(mapper::list) }

    override suspend fun game(id: String) = remoteSource.game(id).map(mapper::map)
}
