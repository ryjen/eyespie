package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.testsupport.testGameId
import com.micrantha.eyespie.testsupport.testGameSnapshot
import com.micrantha.eyespie.testsupport.testGameSummary
import com.micrantha.eyespie.testsupport.testPlayableThingSummary
import com.micrantha.eyespie.testsupport.testThingId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class GameDetailFeatureTest {
    @Test
    fun reducer_rejects_stale_load_completion() {
        val current = GameDetailContent("Current", emptyList())
        val stale = GameDetailContent("Stale", emptyList())
        val loading = GameDetailReducer.reduce(GameDetailState(content = current, loading = false), GameDetailIntent.Load)
        val newerLoad = GameDetailReducer.reduce(loading, GameDetailIntent.Load)
        val late = GameDetailReducer.reduce(newerLoad, GameDetailIntent.ContentLoaded(loading.loadGeneration, stale))
        assertEquals(current, late.content)
        assertTrue(late.loading)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun factory_maps_snapshot_and_emits_semantic_navigation() = runTest {
        val snapshot = testGameSnapshot(
            games = listOf(testGameSummary(name = "Lynn Valley", things = listOf(testPlayableThingSummary("Find the red sign", bestSimilarity = 0.3)))),
        )
        val capabilities = FakeGameDetailCapabilities(snapshot)
        val outputs = mutableListOf<GameDetailOutput>()
        val interactor = GameDetailFactory(capabilities, capabilities, outputs::add).create(this, testGameId)
        interactor.dispatch(GameDetailIntent.Load)
        advanceUntilIdle()
        assertEquals("Lynn Valley", interactor.state.value.content?.name)
        assertEquals("Find the red sign", interactor.state.value.content?.things?.single()?.clueText)
        interactor.dispatch(GameDetailIntent.PlaySelected(testThingId))
        interactor.dispatch(GameDetailIntent.Back)
        assertEquals(listOf(GameDetailOutput.PlayRequested(testGameId, testThingId), GameDetailOutput.Closed), outputs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun local_creator_share_emits_feedback_effect() = runTest {
        val snapshot = testGameSnapshot(games = listOf(testGameSummary(name = "Lynn Valley", localCreator = true)))
        val capabilities = FakeGameDetailCapabilities(snapshot, GameDetailShareResult.Shared)
        val interactor = GameDetailFactory(capabilities, capabilities, {}).create(this, testGameId)
        interactor.dispatch(GameDetailIntent.Load)
        advanceUntilIdle()
        interactor.dispatch(GameDetailIntent.ShareSelected)
        assertTrue(interactor.state.value.shareInProgress)
        advanceUntilIdle()
        assertFalse(interactor.state.value.shareInProgress)
        assertEquals(GameDetailEffect.ShareFinished(GameDetailShareResult.Shared), interactor.effects.first())
        assertEquals(listOf(testGameId to "Lynn Valley"), capabilities.shares)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun imported_game_does_not_start_share_operation() = runTest {
        val capabilities = FakeGameDetailCapabilities(
            testGameSnapshot(games = listOf(testGameSummary(name = "Imported", localCreator = false))),
            GameDetailShareResult.Shared,
        )
        val interactor = GameDetailFactory(capabilities, capabilities, {}).create(this, testGameId)
        interactor.dispatch(GameDetailIntent.Load)
        advanceUntilIdle()
        interactor.dispatch(GameDetailIntent.ShareSelected)
        advanceUntilIdle()
        assertFalse(interactor.state.value.shareInProgress)
        assertTrue(capabilities.shares.isEmpty())
    }
}

private class FakeGameDetailCapabilities(
    private val snapshot: LocalGameSnapshot,
    private val shareResult: GameDetailShareResult = GameDetailShareResult.Unavailable,
) : GameSnapshotLoader, GameSharer {
    val shares = mutableListOf<Pair<GameId, String>>()
    override suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot> = LocalGameResult.Success(snapshot)
    override suspend fun share(gameId: GameId, gameName: String): GameDetailShareResult {
        shares += gameId to gameName
        return shareResult
    }
}
