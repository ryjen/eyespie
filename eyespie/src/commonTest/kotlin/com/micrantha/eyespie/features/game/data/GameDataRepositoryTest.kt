package com.micrantha.eyespie.features.game.data

import com.micrantha.eyespie.features.game.data.mapping.GameDomainMapper
import com.micrantha.eyespie.features.game.data.source.GameRemoteSource
import com.micrantha.eyespie.graphql.GameListQuery
import com.micrantha.eyespie.graphql.GameNodeQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class GameDataRepositoryTest {

    private class FakeGameRemoteSource : GameRemoteSource {
        var gamesResult: Result<List<GameListQuery.Node>> = Result.success(emptyList())
        var gameResult: Result<GameNodeQuery.GameNode> = Result.failure(Exception("Not found"))

        override suspend fun games() = gamesResult
        override suspend fun game(id: String) = gameResult
    }

    private val remoteSource = FakeGameRemoteSource()
    private val mapper = GameDomainMapper()
    private val repository = GameDataRepository(remoteSource, mapper)

    @Test
    fun `games should return success when remote returns success`() = runTest {
        remoteSource.gamesResult = Result.success(emptyList())

        val result = repository.games()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `games should return failure when remote returns failure`() = runTest {
        remoteSource.gamesResult = Result.failure(Exception("Remote error"))

        val result = repository.games()

        assertTrue(result.isFailure)
    }
}
