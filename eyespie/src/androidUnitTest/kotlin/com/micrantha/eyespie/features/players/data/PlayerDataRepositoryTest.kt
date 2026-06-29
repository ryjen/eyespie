package com.micrantha.eyespie.features.players.data

import com.micrantha.eyespie.core.data.system.mapping.LocationDomainMapper
import com.micrantha.eyespie.features.players.data.mapping.PlayerDomainMapper
import com.micrantha.eyespie.features.players.data.model.PlayerResponse
import com.micrantha.eyespie.features.players.data.source.PlayerRemoteSource
import com.micrantha.eyespie.features.players.data.source.PlayersLocalSource
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class PlayerDataRepositoryTest {

    private val remoteSource = mockk<PlayerRemoteSource>()
    private val localSource = mockk<PlayersLocalSource>(relaxUnitFun = true)
    private val mapper = PlayerDomainMapper(LocationDomainMapper())
    private val repository = PlayerDataRepository(remoteSource, localSource, mapper)

    @Test
    fun `players should save to local on success`() = runTest {
        val remotePlayers = listOf(PlayerResponse(id = "1", user_id = "u1", created_at = "2023-01-01T00:00:00Z", first_name = "f", last_name = "l", total_score = 0))
        coEvery { remoteSource.players() } returns Result.success(remotePlayers)

        repository.players()

        verify { localSource.saveAll(remotePlayers) }
    }
}
