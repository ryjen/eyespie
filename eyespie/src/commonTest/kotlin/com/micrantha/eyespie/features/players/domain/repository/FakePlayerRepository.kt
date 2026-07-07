package com.micrantha.eyespie.features.players.domain.repository

import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.players.domain.entities.PlayerList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakePlayerRepository : PlayerRepository {
    var playerResult: Result<Player> = Result.failure(Exception("Not set"))
    var playersResult: Result<PlayerList> = Result.success(emptyList())

    override fun players(): Flow<Result<PlayerList>> = flowOf(playersResult)

    override fun nearby(location: Location.Point): Flow<Result<PlayerList>> = flowOf(playersResult)

    override fun player(userId: String): Flow<Result<Player>> = flowOf(playerResult)

    override suspend fun create(
        userId: String,
        firstName: String,
        lastName: String,
        nickName: String
    ): Result<Player> = playerResult
}
