package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.testsupport.testGameId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class HomeFeatureTest {
    @Test
    fun reducer_rejects_stale_refresh_completion() {
        val current = HomeContent("Current", "current", emptyList())
        val stale = HomeContent("Stale", "stale", emptyList())
        val refreshing = HomeReducer.reduce(HomeState(content = current, loading = false), HomeIntent.Refresh)
        val late = HomeReducer.reduce(
            HomeReducer.reduce(refreshing, HomeIntent.Refresh),
            HomeIntent.ContentLoaded(refreshing.refreshGeneration, stale),
        )

        assertEquals(current, late.content)
        assertTrue(late.loading)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun factory_injects_port_and_interactor_reduces_before_loading() = runTest {
        val expected = HomeContent("Agent", "player-1", emptyList())
        val port = FakeHomePort(expected)
        val outputs = mutableListOf<HomeOutput>()
        val interactor = HomeFactory(port, outputs::add).create(
            scope = this,
            initialState = HomeState(loading = false),
        )

        interactor.dispatch(HomeIntent.Refresh)
        assertTrue(interactor.state.value.loading)
        advanceUntilIdle()

        assertFalse(interactor.state.value.loading)
        assertEquals(expected, interactor.state.value.content)
        assertEquals(1, port.loads)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun imported_game_refreshes_library_and_records_result() = runTest {
        val expected = HomeContent("Agent", "player-1", emptyList())
        val port = FakeHomePort(expected, HomeImportResult.Imported)
        val interactor = HomeFactory(port, {}).create(
            scope = this,
            initialState = HomeState(content = expected, loading = false),
        )

        interactor.dispatch(HomeIntent.ImportSelected)
        assertTrue(interactor.state.value.importInProgress)
        advanceUntilIdle()

        assertFalse(interactor.state.value.importInProgress)
        assertEquals(HomeImportResult.Imported, interactor.state.value.importResult)
        assertEquals(1, port.imports)
        assertEquals(1, port.loads)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancelled_import_is_silent_and_does_not_refresh() = runTest {
        val expected = HomeContent("Agent", "player-1", emptyList())
        val port = FakeHomePort(expected, HomeImportResult.Cancelled)
        val interactor = HomeFactory(port, {}).create(
            scope = this,
            initialState = HomeState(content = expected, loading = false),
        )

        interactor.dispatch(HomeIntent.ImportSelected)
        advanceUntilIdle()

        assertNull(interactor.state.value.importResult)
        assertEquals(1, port.imports)
        assertEquals(0, port.loads)
    }

    @Test
    fun factory_wires_semantic_outputs_without_app_routes() = runTest {
        val outputs = mutableListOf<HomeOutput>()
        val interactor = HomeFactory(FakeHomePort(HomeContent("Agent", "id", emptyList())), outputs::add)
            .create(this)

        interactor.dispatch(HomeIntent.UtilitySelected)
        interactor.dispatch(HomeIntent.CreateSelected)
        interactor.dispatch(HomeIntent.GameSelected(testGameId))

        assertEquals(
            listOf(
                HomeOutput.UtilityRequested,
                HomeOutput.CreateRequested,
                HomeOutput.GameRequested(testGameId),
            ),
            outputs,
        )
    }
}

private class FakeHomePort(
    private val content: HomeContent,
    private val importResult: HomeImportResult = HomeImportResult.Unavailable,
) : HomePort {
    var loads = 0
    var imports = 0

    override suspend fun load(): LocalGameResult<HomeContent> {
        loads += 1
        return LocalGameResult.Success(content)
    }

    override suspend fun importGame(): HomeImportResult {
        imports += 1
        return importResult
    }
}
