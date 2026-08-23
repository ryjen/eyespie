package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.testsupport.testGameId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
    fun factory_maps_domain_snapshot_and_reduces_before_loading() = runTest {
        val expected = HomeContent("Agent", "player-1", emptyList())
        val capabilities = FakeHomeCapabilities(snapshot())
        val interactor = homeFactory(capabilities, {}).create(this, HomeState(loading = false))
        interactor.dispatch(HomeIntent.Refresh)
        assertTrue(interactor.state.value.loading)
        advanceUntilIdle()
        assertFalse(interactor.state.value.loading)
        assertEquals(expected, interactor.state.value.content)
        assertEquals(1, capabilities.loads)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun selected_game_is_previewed_without_import_or_library_refresh() = runTest {
        val expected = HomeContent("Agent", "player-1", emptyList())
        val preview = HomeImportPreview("Road Trip", 3, "creator-1234", "game-5678")
        val capabilities = FakeHomeCapabilities(snapshot(), HomeImportPreparationResult.Ready(preview), HomeImportResult.Imported)
        val interactor = homeFactory(capabilities, {}).create(this, HomeState(content = expected, loading = false))
        interactor.dispatch(HomeIntent.ImportSelected)
        assertTrue(interactor.state.value.importInProgress)
        advanceUntilIdle()
        assertFalse(interactor.state.value.importInProgress)
        assertEquals(preview, interactor.state.value.importPreview)
        assertEquals(1, capabilities.prepares)
        assertEquals(0, capabilities.confirms)
        assertEquals(0, capabilities.loads)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun confirmed_import_refreshes_library_and_emits_feedback() = runTest {
        val expected = HomeContent("Agent", "player-1", emptyList())
        val preview = HomeImportPreview("Road Trip", 1, "creator-1234", "game-5678")
        val capabilities = FakeHomeCapabilities(snapshot(), HomeImportPreparationResult.Ready(preview), HomeImportResult.Imported)
        val interactor = homeFactory(capabilities, {}).create(this, HomeState(content = expected, loading = false))
        interactor.dispatch(HomeIntent.ImportSelected)
        advanceUntilIdle()
        interactor.dispatch(HomeIntent.ImportConfirmed)
        assertTrue(interactor.state.value.importInProgress)
        advanceUntilIdle()
        assertFalse(interactor.state.value.importInProgress)
        assertNull(interactor.state.value.importPreview)
        assertEquals(HomeEffect.ImportFinished(HomeImportResult.Imported), interactor.effects.first())
        assertEquals(1, capabilities.prepares)
        assertEquals(1, capabilities.confirms)
        assertEquals(1, capabilities.loads)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancelled_preview_clears_pending_import_without_refresh() = runTest {
        val expected = HomeContent("Agent", "player-1", emptyList())
        val preview = HomeImportPreview("Road Trip", 1, "creator-1234", "game-5678")
        val capabilities = FakeHomeCapabilities(snapshot(), HomeImportPreparationResult.Ready(preview))
        val interactor = homeFactory(capabilities, {}).create(this, HomeState(content = expected, loading = false))
        interactor.dispatch(HomeIntent.ImportSelected)
        advanceUntilIdle()
        interactor.dispatch(HomeIntent.ImportPreviewCancelled)
        assertNull(interactor.state.value.importPreview)
        assertEquals(1, capabilities.cancels)
        assertEquals(0, capabilities.confirms)
        assertEquals(0, capabilities.loads)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun disposal_clears_pending_import_candidate() = runTest {
        val expected = HomeContent("Agent", "player-1", emptyList())
        val preview = HomeImportPreview("Road Trip", 1, "creator-1234", "game-5678")
        val capabilities = FakeHomeCapabilities(snapshot(), HomeImportPreparationResult.Ready(preview))
        val interactor = homeFactory(capabilities, {}).create(this, HomeState(content = expected, loading = false))
        interactor.dispatch(HomeIntent.ImportSelected)
        advanceUntilIdle()
        interactor.dispose()
        assertEquals(preview, interactor.state.value.importPreview)
        assertEquals(1, capabilities.cancels)
        assertEquals(0, capabilities.confirms)
        assertEquals(0, capabilities.loads)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancelled_document_selection_is_silent_and_does_not_refresh() = runTest {
        val expected = HomeContent("Agent", "player-1", emptyList())
        val capabilities = FakeHomeCapabilities(snapshot(), HomeImportPreparationResult.Terminal(HomeImportResult.Cancelled))
        val interactor = homeFactory(capabilities, {}).create(this, HomeState(content = expected, loading = false))
        interactor.dispatch(HomeIntent.ImportSelected)
        advanceUntilIdle()
        assertNull(interactor.state.value.importPreview)
        assertEquals(1, capabilities.prepares)
        assertEquals(0, capabilities.confirms)
        assertEquals(0, capabilities.loads)
    }

    @Test
    fun factory_wires_semantic_outputs_without_app_routes() = runTest {
        val outputs = mutableListOf<HomeOutput>()
        val interactor = homeFactory(FakeHomeCapabilities(snapshot()), outputs::add).create(this)
        interactor.dispatch(HomeIntent.UtilitySelected)
        interactor.dispatch(HomeIntent.CreateSelected)
        interactor.dispatch(HomeIntent.GameSelected(testGameId))
        assertEquals(
            listOf(HomeOutput.UtilityRequested, HomeOutput.CreateRequested, HomeOutput.GameRequested(testGameId)),
            outputs,
        )
    }
}

private fun homeFactory(capabilities: FakeHomeCapabilities, output: (HomeOutput) -> Unit): HomeFactory = HomeFactory(
    snapshotLoader = capabilities,
    importPreparer = capabilities,
    importConfirmer = capabilities,
    importCanceller = capabilities,
    output = output,
)

private fun snapshot(): LocalGameSnapshot = LocalGameSnapshot(
    identity = PlayerIdentity(PlayerId("player-1"), "Agent"),
    games = emptyList(),
)

private class FakeHomeCapabilities(
    private val snapshot: LocalGameSnapshot,
    private val preparation: HomeImportPreparationResult = HomeImportPreparationResult.Terminal(HomeImportResult.Unavailable),
    private val confirmResult: HomeImportResult = HomeImportResult.Unavailable,
) : GameSnapshotLoader, GameImportPreparer, GameImportConfirmer, GameImportCanceller {
    var loads = 0
    var prepares = 0
    var confirms = 0
    var cancels = 0

    override suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot> {
        loads += 1
        return LocalGameResult.Success(snapshot)
    }
    override suspend fun prepareImport(): HomeImportPreparationResult { prepares += 1; return preparation }
    override suspend fun confirmImport(): HomeImportResult { confirms += 1; return confirmResult }
    override fun cancelImport() { cancels += 1 }
}
