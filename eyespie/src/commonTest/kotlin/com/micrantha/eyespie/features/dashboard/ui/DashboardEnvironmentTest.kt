package com.micrantha.eyespie.features.dashboard.ui

import com.micrantha.bluebell.arch.FakeDispatcher
import com.micrantha.bluebell.ui.model.UiResult
import com.micrantha.eyespie.core.ui.FakeScreenContext
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.features.dashboard.ui.DashboardAction.GuessThing
import com.micrantha.eyespie.features.dashboard.ui.DashboardAction.Load
import com.micrantha.eyespie.features.dashboard.ui.DashboardAction.LoadError
import com.micrantha.eyespie.features.dashboard.ui.DashboardAction.Loaded
import com.micrantha.eyespie.features.dashboard.ui.DashboardAction.ScanNewThing
import com.micrantha.eyespie.features.dashboard.ui.DashboardAction.SyncCountUpdated
import com.micrantha.eyespie.features.dashboard.ui.usecase.DashboardLoadUseCase
import com.micrantha.eyespie.features.game.ui.component.GameAction
import com.micrantha.eyespie.features.game.ui.detail.GameDetailScreenArg
import com.micrantha.eyespie.features.game.ui.detail.GameDetailsScreen
import com.micrantha.eyespie.features.guess.ui.ScanGuessArgs
import com.micrantha.eyespie.features.guess.ui.ScanGuessScreen
import com.micrantha.eyespie.features.scan.data.FakeCaptureSyncRepository
import com.micrantha.eyespie.features.scan.ui.capture.ScanCaptureScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
class DashboardEnvironmentTest {

    private class FakeLoadUseCase : DashboardLoadUseCase {
        val flow = MutableSharedFlow<Result<Loaded>>()
        override fun invoke(): Flow<Result<Loaded>> = flow
    }

    private val dispatcher = FakeDispatcher(CoroutineScope(UnconfinedTestDispatcher()))
    private val context = FakeScreenContext(dispatcher = dispatcher)
    private val captureSyncRepository = FakeCaptureSyncRepository()

    private val di = DI {
        bindFactory { arg: GameDetailScreenArg -> GameDetailsScreen(context, arg) }
        bindProvider { ScanCaptureScreen() }
        bindFactory { arg: ScanGuessArgs -> ScanGuessScreen(arg) }
    }

    init {
        context.di = di
    }

    private val useCase = FakeLoadUseCase()
    private val environment = DashboardEnvironment(context, useCase, captureSyncRepository)

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
        val state = DashboardState(status = UiResult.Default)
        val action = Load

        val newState = environment.reduce(state, action)

        assertIs<UiResult.Busy>(newState.status)
    }

    @Test
    fun `reduce Loaded action should return Ready status`() {
        val state = DashboardState(status = UiResult.Busy())
        val action = Loaded(
            nearbyThings = emptyList(),
            nearbyPlayers = emptyList(),
            friends = emptyList()
        )

        val newState = environment.reduce(state, action)

        assertIs<UiResult.Ready<*>>(newState.status)
    }

    @Test
    fun `reduce LoadError action should return Failure status`() {
        val state = DashboardState(status = UiResult.Busy())
        val action = LoadError

        val newState = environment.reduce(state, action)

        assertIs<UiResult.Failure>(newState.status)
    }

    @Test
    fun `invoke GameClicked action should navigate to GameDetailsScreen`() = runTest {
        val game = com.micrantha.eyespie.domain.entities.Game.Listing(
            id = "1",
            nodeId = "1",
            name = "Title",
            createdAt = Instant.parse("2023-01-01T00:00:00Z"),
            expiresAt = Instant.parse("2023-01-02T00:00:00Z"),
            totalThings = 0,
            totalPlayers = 0
        )
        val action = GameAction.GameClicked(game)

        environment.invoke(action, DashboardState())

        assertIs<GameDetailsScreen>(context.router.lastNavigatedTo)
    }

    @Test
    fun `invoke ScanNewThing action should navigate to ScanCaptureScreen`() = runTest {
        val action = ScanNewThing

        environment.invoke(action, DashboardState())

        assertIs<ScanCaptureScreen>(context.router.lastNavigatedTo)
    }

    @Test
    fun `invoke GuessThing action should navigate to ScanGuessScreen`() = runTest {
        val thing = Thing.Listing(
            id = "1",
            createdAt = Instant.parse("2023-01-01T00:00:00Z"),
            nodeId = "1",
            guessed = false,
            imageUrl = "url",
        )
        val action = GuessThing(thing)

        environment.invoke(action, DashboardState())

        assertIs<ScanGuessScreen>(context.router.lastNavigatedTo)
    }

    @Test
    fun `invoke Load action should call use case and dispatch Loaded`() = runTest {
        val action = Load
        val loaded = Loaded(emptyList(), emptyList(), emptyList())
        
        environment.invoke(action, DashboardState())
        
        useCase.flow.emit(Result.success(loaded))

        assertIs<Loaded>(dispatcher.actions.find { it is Loaded })
    }

    @Test
    fun `invoke Load action should call use case and dispatch LoadError on failure`() = runTest {
        val action = Load
        
        environment.invoke(action, DashboardState())
        
        useCase.flow.emit(Result.failure(Exception("Error")))

        assertIs<LoadError>(dispatcher.actions.find { it is LoadError })
    }

    @Test
    fun `invoke Load action should monitor capture sync count`() = runTest {
        val action = Load
        
        environment.invoke(action, DashboardState())
        
        captureSyncRepository.countFlow.value = 5

        val syncAction = dispatcher.actions.filterIsInstance<SyncCountUpdated>().last()
        assertEquals(5, syncAction.count)
    }
}
