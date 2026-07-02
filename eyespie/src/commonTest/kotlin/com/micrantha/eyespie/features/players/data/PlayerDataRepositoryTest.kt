package com.micrantha.eyespie.features.players.data

import com.micrantha.eyespie.core.data.system.mapping.LocationDomainMapper
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.features.players.data.mapping.PlayerDomainMapper
import com.micrantha.eyespie.features.players.data.model.PlayerResponse
import com.micrantha.eyespie.features.players.data.source.PlayerRemoteSource
import com.micrantha.eyespie.features.players.data.source.PlayersLocalSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerDataRepositoryTest {

    private class FakePlayerRemoteSource : PlayerRemoteSource {
        var playersResult: Result<List<PlayerResponse>> = Result.success(emptyList())
        var playerResult: Result<PlayerResponse> = Result.failure(Exception("Not found"))
        var createResult: Result<Unit> = Result.success(Unit)
        var nearbyResult: Result<List<PlayerResponse>> = Result.success(emptyList())

        var createCalledWith: List<String>? = null

        override suspend fun players() = playersResult

        override suspend fun player(id: String) = playerResult

        override suspend fun create(
            userId: String,
            firstName: String,
            lastName: String,
            nickName: String
        ): Result<Unit> {
            createCalledWith = listOf(userId, firstName, lastName, nickName)
            return createResult
        }

        override suspend fun nearby(location: Location.Point) = nearbyResult
    }

    private class FakePlayersLocalSource : PlayersLocalSource {
        var players: List<PlayerResponse> = emptyList()
        var saveAllCalledWith: List<PlayerResponse>? = null

        override fun getAll(): Result<List<PlayerResponse>> = Result.success(players)

        override fun saveAll(players: List<PlayerResponse>): Result<Unit> {
            saveAllCalledWith = players
            this.players = players
            return Result.success(Unit)
        }
    }

    private val remoteSource = FakePlayerRemoteSource()
    private val localSource = FakePlayersLocalSource()
    private val mapper = PlayerDomainMapper(LocationDomainMapper())
    private val repository = PlayerDataRepository(remoteSource, localSource, mapper)

    @Test
    fun `players should save to local on success`() = runTest {
        val remotePlayers = listOf(
            PlayerResponse(
                id = "1",
                user_id = "u1",
                created_at = "2023-01-01T00:00:00Z",
                first_name = "f",
                last_name = "l",
                total_score = 0,
                nodeId = null,
                email = null,
                last_location = null,
                nick_name = null
            )
        )
        remoteSource.playersResult = Result.success(remotePlayers)

        repository.players()

        assertEquals(remotePlayers, localSource.saveAllCalledWith)
    }

    @Test
    fun `players should fallback to local on failure`() = runTest {
        val localPlayers = listOf(
            PlayerResponse(
                id = "1",
                user_id = "u1",
                created_at = "2023-01-01T00:00:00Z",
                first_name = "f",
                last_name = "l",
                total_score = 0,
                nodeId = null,
                email = null,
                last_location = null,
                nick_name = null
            )
        )
        localSource.players = localPlayers
        remoteSource.playersResult = Result.failure(Exception("Network error"))

        val result = repository.players()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
    }

    @Test
    fun `player by id should fetch from remote`() = runTest {
        val player = PlayerResponse(
            id = "1",
            user_id = "u1",
            created_at = "2023-01-01T00:00:00Z",
            first_name = "f",
            last_name = "l",
            total_score = 0,
            nodeId = null,
            email = null,
            last_location = null,
            nick_name = null
        )
        remoteSource.playerResult = Result.success(player)

        val result = repository.player("u1")

        assertTrue(result.isSuccess)
        assertEquals("1", result.getOrThrow().id)
    }

    @Test
    fun `create player should call remote source and fetch it`() = runTest {
        val player = PlayerResponse(
            id = "1",
            user_id = "u1",
            created_at = "2023-01-01T00:00:00Z",
            first_name = "f",
            last_name = "l",
            total_score = 0,
            nodeId = null,
            email = null,
            last_location = null,
            nick_name = null
        )
        remoteSource.createResult = Result.success(Unit)
        remoteSource.playerResult = Result.success(player)

        val result = repository.create("u1", "f", "l", "n")

        assertTrue(result.isSuccess)
        assertEquals(listOf("u1", "f", "l", "n"), remoteSource.createCalledWith)
    }

    @Test
    fun `nearby should save to local on success`() = runTest {
        val remotePlayers = listOf(
            PlayerResponse(
                id = "1",
                user_id = "u1",
                created_at = "2023-01-01T00:00:00Z",
                first_name = "f",
                last_name = "l",
                total_score = 0,
                nodeId = null,
                email = null,
                last_location = null,
                nick_name = null
            )
        )
        remoteSource.nearbyResult = Result.success(remotePlayers)

        repository.nearby(Location.Point(0.0, 0.0))

        assertEquals(remotePlayers, localSource.saveAllCalledWith)
    }
}
