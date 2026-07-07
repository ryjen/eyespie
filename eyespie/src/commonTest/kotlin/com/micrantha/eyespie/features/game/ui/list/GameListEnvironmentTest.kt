package com.micrantha.eyespie.features.game.ui.list

import com.micrantha.bluebell.arch.FakeDispatcher
import com.micrantha.bluebell.ui.model.UiResult
import com.micrantha.eyespie.core.ui.FakeScreen
import com.micrantha.eyespie.core.ui.FakeScreenContext
import com.micrantha.eyespie.domain.repository.FakeGameRepository
import com.micrantha.eyespie.features.game.ui.component.GameAction.GameClicked
import com.micrantha.eyespie.features.game.ui.create.GameCreateScreen
import com.micrantha.eyespie.features.game.ui.detail.GameDetailScreenArg
import com.micrantha.eyespie.features.game.ui.detail.GameDetailsScreen
import com.micrantha.eyespie.features.game.ui.list.GameListAction.Load
import com.micrantha.eyespie.features.game.ui.list.GameListAction.Loaded
import com.micrantha.eyespie.features.game.ui.list.GameListAction.NewGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.kodein.di.DI
import org.kodein.di.bindFactory
import org.kodein.di.bindProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GameListEnvironmentTest {

    private val repository = FakeGameRepository()
    private val dispatcher = FakeDispatcher()
    private val context = FakeScreenContext(dispatcher = dispatcher)

    private val di = DI {
        bindProvider { GameCreateScreen() }
        bindFactory { arg: GameDetailScreenArg -> GameDetailsScreen(context, arg) }
    }

    init {
        context.di = di
    }

    private val environment = GameListEnvironment(context, repository)

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
        val state = GameListState(status = UiResult.Default)
        val action = Load

        val newState = environment.reduce(state, action)

        assertIs<UiResult.Busy>(newState.status)
    }

    @Test
    fun `reduce Loaded action should return Ready status`() {
        val state = GameListState(status = UiResult.Busy())
        val games = listOf(
            com.micrantha.eyespie.domain.entities.Game.Listing(
                id = "1",
                nodeId = "1",
                name = "Test Game",
                createdAt = Instant.parse("2023-01-01T00:00:00Z"),
                expiresAt = Instant.parse("2023-01-02T00:00:00Z"),
                totalThings = 0,
                totalPlayers = 0
            )
        )
        val action = Loaded(games)

        val newState = environment.reduce(state, action)

        assertIs<UiResult.Ready<*>>(newState.status)
    }

    @Test
    fun `invoke Load action should call repository and dispatch Loaded`() = runTest {
        val games = emptyList<com.micrantha.eyespie.domain.entities.Game.Listing>()
        repository.gamesResult = Result.success(games)

        environment.invoke(Load, GameListState())

        assertEquals(1, dispatcher.actions.size)
        assertIs<Loaded>(dispatcher.actions.first())
    }

    @Test
    fun `invoke NewGame action should navigate to GameCreateScreen`() = runTest {
        environment.invoke(NewGame, GameListState())

        assertIs<GameCreateScreen>(context.router.lastNavigatedTo)
    }

    @Test
    fun `invoke GameClicked action should navigate to GameDetailsScreen`() = runTest {
        val game = com.micrantha.eyespie.domain.entities.Game.Listing(
            id = "1",
            nodeId = "1",
            name = "Test Game",
            createdAt = Instant.parse("2023-01-01T00:00:00Z"),
            expiresAt = Instant.parse("2023-01-02T00:00:00Z"),
            totalThings = 0,
            totalPlayers = 0
        )
        val action = GameClicked(game)

        environment.invoke(action, GameListState())

        assertIs<GameDetailsScreen>(context.router.lastNavigatedTo)
    }
}
