package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.Game
import com.micrantha.eyespie.domain.entities.GameList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class FakeGameRepository : GameRepository {
    var gamesResult: Result<GameList> = Result.success(emptyList())
    var gameResult: Result<Game> = Result.failure(Exception("Not found"))

    override fun games(): Flow<Result<GameList>> = flowOf(gamesResult)

    override fun game(id: String): Flow<Result<Game>> = flowOf(gameResult)
}
