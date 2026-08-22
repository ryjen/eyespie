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
    fun selected_game_is_previewed_without_import_or_library_refresh() = runTest {
        val expected = HomeContent("Agent", "player-1", emptyList())
        val preview = HomeImportPreview("Road Trip", 3, "creator-1234", "game-5678")
        val port = FakeHomePort(
            content = expected,
            preparation = HomeImportPreparationResult.Ready(preview),
            confirmResult = HomeImportResult.Imported,
        )
        val interactor = HomeFactory(port, {}).create(
            scope = this,
            initialState = HomeState(content = expected, loading = false),
        )

        interactor.dispatch(HomeIntent.ImportSelected)
        assertTrue(interactor.state.value.importInProgress)
        advanceUntilIdle()

        assertFalse(interactor.state.value.importInProgress)
        assertEquals(preview, interactor.state.value.importPreview)
        assertNull(interactor.state.value.importResult)
        assertEquals(1, port.prepares)
        assertEquals(0, port.confirms)
        assertEquals(0, port.loads)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun confirmed_import_refreshes_library_and_records_result() = runTest {
        val expected = HomeContent("Agent", "player-1", emptyList())
        val preview = HomeImportPreview("Road Trip", 1, "creator-1234", "game-5678")
        val port = FakeHomePort(
            content = expected,
            preparation = HomeImportPreparationResult.Ready(preview),
            confirmResult = HomeImportResult.Imported,
        )
        val interactor = HomeFactory(port, {}).create(
            scope = this,
            initialState = HomeState(content = expected, loading = false),
        )

        interactor.dispatch(HomeIntent.ImportSelected)
        advanceUntilIdle()
        interactor.dispatch(HomeIntent.ImportConfirmed)
        assertTrue(interactor.state.value.importInProgress)
        advanceUntilIdle()

        assertFalse(interactor.state.value.importInProgress)
        assertNull(interactor.state.value.importPreview)
        assertEquals(HomeImportResult.Imported, interactor.state.value.importResult)
        assertEquals(1, port.prepares)
        assertEquals(1, port.confirms)
        assertEquals(1, port.loads)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancelled_preview_clears_pending_import_without_refresh() = runTest {
        val expected = HomeContent("Agent", "player-1", emptyList())
        val preview = HomeImportPreview("Road Trip", 1, "creator-1234", "game-5678")
        val port = FakeHomePort(
            content = expected,
            preparation = HomeImportPreparationResult.Ready(preview),
        )
        val interactor = HomeFactory(port, {}).create(
            scope = this,
            initialState = HomeState(content = expected, loading = false),
        )

        interactor.dispatch(HomeIntent.ImportSelected)
        advanceUntilIdle()
        interactor.dispatch(HomeIntent.ImportPreviewCancelled)

        assertNull(interactor.state.value.importPreview)
        assertNull(interactor.state.value.importResult)
        assertEquals(1, port.cancels)
        assertEquals(0, port.confirms)
        assertEquals(0, port.loads)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancelled_document_selection_is_silent_and_does_not_refresh() = runTest {
        val expected = HomeContent("Agent", "player-1", emptyList())
        val port = FakeHomePort(
            content = expected,
            preparation = HomeImportPreparationResult.Terminal(HomeImportResult.Cancelled),
        )
        val interactor = HomeFactory(port, {}).create(
            scope = this,
            initialState = HomeState(content = expected, loading = false),
        )

        interactor.dispatch(HomeIntent.ImportSelected)
        advanceUntilIdle()

        assertNull(interactor.state.value.importPreview)
        assertNull(interactor.state.value.importResult)
        assertEquals(1, port.prepares)
        assertEquals(0, port.confirms)
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
    private val preparation: HomeImportPreparationResult =
        HomeImportPreparationResult.Terminal(HomeImportResult.Unavailable),
    private val confirmResult: HomeImportResult = HomeImportResult.Unavailable,
) : HomePort {
    var loads = 0
    var prepares = 0
    var confirms = 0
    var cancels = 0

    override suspend fun load(): LocalGameResult<HomeContent> {
        loads += 1
        return LocalGameResult.Success(content)
    }

    override suspend fun prepareImport(): HomeImportPreparationResult {
        prepares += 1
        return preparation
    }

    override suspend fun confirmImport(): HomeImportResult {
        confirms += 1
        return confirmResult
    }

    override fun cancelImport() {
        cancels += 1
    }
}
