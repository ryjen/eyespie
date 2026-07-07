package com.micrantha.eyespie.features.game.ui.detail

import com.micrantha.bluebell.arch.FakeDispatcher
import com.micrantha.bluebell.ui.model.UiResult
import com.micrantha.eyespie.core.ui.FakeScreenContext
import com.micrantha.eyespie.domain.repository.FakeGameRepository
import com.micrantha.eyespie.features.game.ui.detail.GameDetailsAction.Load
import com.micrantha.eyespie.features.game.ui.detail.GameDetailsAction.Loaded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.kodein.di.DI
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GameDetailsEnvironmentTest {

    private val repository = FakeGameRepository()
    private val dispatcher = FakeDispatcher()
    private val context = FakeScreenContext(dispatcher = dispatcher)

    private val environment = GameDetailsEnvironment(context, repository)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reduce Load action should return Busy status`() {
        val state = GameDetailsState(status = UiResult.Default)
        val action = Load("1")

        val newState = environment.reduce(state, action)

        assertIs<UiResult.Busy>(newState.status)
    }

    @Test
    fun `reduce Loaded action should return Ready status`() {
        val state = GameDetailsState(status = UiResult.Busy())
        val game = com.micrantha.eyespie.domain.entities.Game(
            id = "1",
            name = "Test Game",
            createdAt = Instant.parse("2023-01-01T00:00:00Z"),
            expires = Instant.parse("2023-01-02T00:00:00Z"),
            limits = com.micrantha.eyespie.domain.entities.Game.Limits(
                player = 0..10,
                thing = 0..10
            ),
            players = emptyList(),
            things = emptyList(),
            turnDuration = 1.hours
        )
        val action = Loaded(game)

        val newState = environment.reduce(state, action)

        assertIs<UiResult.Ready<*>>(newState.status)
        assertEquals(game, newState.game)
    }

    @Test
    fun `invoke Load action should call repository and dispatch Loaded`() = runTest {
        val game = com.micrantha.eyespie.domain.entities.Game(
            id = "1",
            name = "Test Game",
            createdAt = Instant.parse("2023-01-01T00:00:00Z"),
            expires = Instant.parse("2023-01-02T00:00:00Z"),
            limits = com.micrantha.eyespie.domain.entities.Game.Limits(
                player = 0..10,
                thing = 0..10
            ),
            players = emptyList(),
            things = emptyList(),
            turnDuration = 1.hours
        )
        repository.gameResult = Result.success(game)

        environment.invoke(Load("1"), GameDetailsState())

        assertEquals(1, dispatcher.actions.size)
        assertIs<Loaded>(dispatcher.actions.first())
    }
}
