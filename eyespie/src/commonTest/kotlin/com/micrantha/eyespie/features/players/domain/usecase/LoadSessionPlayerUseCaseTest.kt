package com.micrantha.eyespie.features.players.domain.usecase

import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.domain.entities.Session
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.features.players.domain.repository.FakePlayerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class LoadSessionPlayerUseCaseTest {

    private val playerRepository = FakePlayerRepository()
    private val currentSession = CurrentSession
    private val useCase = LoadSessionPlayerUseCase(playerRepository, currentSession)

    @Test
    fun `invoke should update currentSession and return player`() = runTest {
        val session = Session(id = "s", accessToken = "a", refreshToken = "r", userId = "u")
        val player = Player("p1", Instant.parse("2023-01-01T00:00:00Z"), Player.Name("f", "l", "n"), "e", Player.Score(0))
        playerRepository.playerResult = Result.success(player)

        val result = useCase(session).first()

        assertTrue(result.isSuccess)
        assertEquals(player, result.getOrThrow())
        assertEquals(player, currentSession.requirePlayer())
        assertEquals("u", currentSession.requireUserId())
    }

    @Test
    fun `invoke should return null if player not found`() = runTest {
        val session = Session(id = "s", accessToken = "a", refreshToken = "r", userId = "u")
        playerRepository.playerResult = Result.failure(Exception("Not found"))

        val result = useCase(session).first()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow() == null)
    }
}
