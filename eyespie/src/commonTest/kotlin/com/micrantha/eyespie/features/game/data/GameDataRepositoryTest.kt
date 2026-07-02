package com.micrantha.eyespie.features.game.data

import com.micrantha.eyespie.features.game.data.mapping.GameDomainMapper
import com.micrantha.eyespie.features.game.data.model.GameData
import com.micrantha.eyespie.features.game.data.source.GameRemoteSource
import com.micrantha.eyespie.features.game.data.source.GamesLocalSource
import com.micrantha.eyespie.graphql.GameListQuery
import com.micrantha.eyespie.graphql.GameNodeQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameDataRepositoryTest {

    private class FakeGameRemoteSource : GameRemoteSource {
        var gamesResult: Result<List<GameListQuery.Node>> = Result.success(emptyList())
        var gameResult: Result<GameNodeQuery.GameNode> = Result.failure(Exception("Not found"))

        override suspend fun games() = gamesResult
        override suspend fun game(id: String) = gameResult
    }

    private class FakeGamesLocalSource : GamesLocalSource {
        var games: List<GameData> = emptyList()
        var saveAllCalledWith: List<GameData>? = null

        override fun getAll(): Result<List<GameData>> = Result.success(games)

        override fun saveAll(games: List<GameData>): Result<Unit> {
            saveAllCalledWith = games
            this.games = games
            return Result.success(Unit)
        }
    }

    private val remoteSource = FakeGameRemoteSource()
    private val localSource = FakeGamesLocalSource()
    private val mapper = GameDomainMapper()
    private val repository = GameDataRepository(remoteSource, localSource, mapper)

    @Test
    fun `games should return success when remote returns success`() = runTest {
        remoteSource.gamesResult = Result.success(emptyList())

        val result = repository.games()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `games should fallback to local when remote returns failure`() = runTest {
        val cachedGames = listOf(GameData("1", "Cached", "2023-01-01T00:00:00Z", null, "u1", 0))
        localSource.games = cachedGames
        remoteSource.gamesResult = Result.failure(Exception("Remote error"))

        val result = repository.games()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        assertEquals("Cached", result.getOrThrow().first().name)
    }
}
