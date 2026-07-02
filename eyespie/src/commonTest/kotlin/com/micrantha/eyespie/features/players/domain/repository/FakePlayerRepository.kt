package com.micrantha.eyespie.features.players.domain.repository

import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.players.domain.entities.PlayerList

class FakePlayerRepository : PlayerRepository {
    var playerResult: Result<Player> = Result.failure(Exception("Not set"))
    var playersResult: Result<PlayerList> = Result.success(emptyList())

    override suspend fun players() = playersResult
    override suspend fun nearby(location: Location.Point) = playersResult
    override suspend fun player(userId: String) = playerResult
    override suspend fun create(userId: String, firstName: String, lastName: String, nickName: String) = playerResult
}
