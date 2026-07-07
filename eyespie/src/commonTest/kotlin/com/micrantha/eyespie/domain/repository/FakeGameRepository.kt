package com.micrantha.eyespie.domain.repository

import com.micrantha.eyespie.domain.entities.Game
import com.micrantha.eyespie.domain.entities.GameList
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class FakeGameRepository : GameRepository {
    var gamesResult: Result<GameList> = Result.success(emptyList())
    var gameResult: Result<Game> = Result.failure(Exception("Not found"))

    override suspend fun games(): Result<GameList> = gamesResult

    override suspend fun game(id: String): Result<Game> = gameResult
}
