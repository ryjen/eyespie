package com.micrantha.eyespie.features.players.domain.repository

import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.players.domain.entities.PlayerList
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {

    fun players(): Flow<Result<PlayerList>>

    fun nearby(location: Location.Point): Flow<Result<PlayerList>>

    fun player(userId: String): Flow<Result<Player>>

    suspend fun create(
        userId: String,
        firstName: String,
        lastName: String,
        nickName: String
    ): Result<Player>
}
