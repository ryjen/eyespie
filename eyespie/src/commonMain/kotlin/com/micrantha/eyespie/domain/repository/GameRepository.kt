package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.Game
import com.micrantha.eyespie.domain.entities.GameList
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun games(): Flow<Result<GameList>>

    fun game(id: String): Flow<Result<Game>>

    //fun nearby(): Flow<Result<GameList>>
}
