package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.testsupport.testGameId
import com.micrantha.eyespie.testsupport.testThingId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class GameDetailFeatureTest {
    @Test
    fun reducer_rejects_stale_load_completion() {
        val current = GameDetailContent("Current", emptyList())
        val stale = GameDetailContent("Stale", emptyList())
        val loading = GameDetailReducer.reduce(
            GameDetailState(content = current, loading = false),
            GameDetailIntent.Load,
        )
        val newerLoad = GameDetailReducer.reduce(loading, GameDetailIntent.Load)
        val late = GameDetailReducer.reduce(
            newerLoad,
            GameDetailIntent.ContentLoaded(loading.loadGeneration, stale),
        )

        assertEquals(current, late.content)
        assertTrue(late.loading)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun factory_loads_content_and_emits_semantic_navigation() = runTest {
        val expected = GameDetailContent(
            name = "Lynn Valley",
            things = listOf(GameDetailThing(testThingId, "Find the red sign", false, 0.3)),
        )
        val capabilities = FakeGameDetailCapabilities(expected)
        val outputs = mutableListOf<GameDetailOutput>()
        val interactor = GameDetailFactory(capabilities, capabilities, outputs::add).create(this, testGameId)

        interactor.dispatch(GameDetailIntent.Load)
        assertTrue(interactor.state.value.loading)
        advanceUntilIdle()

        assertFalse(interactor.state.value.loading)
        assertEquals(expected, interactor.state.value.content)
        assertEquals(listOf(testGameId), capabilities.loads)

        interactor.dispatch(GameDetailIntent.PlaySelected(testThingId))
        interactor.dispatch(GameDetailIntent.Back)

        assertEquals(
            listOf(
                GameDetailOutput.PlayRequested(testGameId, testThingId),
                GameDetailOutput.Closed,
            ),
            outputs,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun local_creator_can_share_through_feature_capability() = runTest {
        val expected = GameDetailContent(
            name = "Lynn Valley",
            things = emptyList(),
            localCreator = true,
        )
        val capabilities = FakeGameDetailCapabilities(expected, GameDetailShareResult.Shared)
        val interactor = GameDetailFactory(capabilities, capabilities, {}).create(this, testGameId)

        interactor.dispatch(GameDetailIntent.Load)
        advanceUntilIdle()
        interactor.dispatch(GameDetailIntent.ShareSelected)
        assertTrue(interactor.state.value.shareInProgress)
        advanceUntilIdle()

        assertFalse(interactor.state.value.shareInProgress)
        assertEquals(GameDetailShareResult.Shared, interactor.state.value.shareResult)
        assertEquals(listOf(testGameId to "Lynn Valley"), capabilities.shares)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun imported_game_does_not_start_share_operation() = runTest {
        val expected = GameDetailContent("Imported", emptyList(), localCreator = false)
        val capabilities = FakeGameDetailCapabilities(expected, GameDetailShareResult.Shared)
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
    private val content: GameDetailContent,
    private val shareResult: GameDetailShareResult = GameDetailShareResult.Unavailable,
) : GameDetailLoader, GameSharer {
    val loads = mutableListOf<GameId>()
    val shares = mutableListOf<Pair<GameId, String>>()

    override suspend fun load(gameId: GameId): LocalGameResult<GameDetailContent> {
        loads += gameId
        return LocalGameResult.Success(content)
    }

    override suspend fun share(gameId: GameId, gameName: String): GameDetailShareResult {
        shares += gameId to gameName
        return shareResult
    }
}
