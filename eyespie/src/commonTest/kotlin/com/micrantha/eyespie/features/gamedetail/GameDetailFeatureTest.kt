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
            localCreator = true,
        )
        val port = FakeGameDetailPort(expected)
        val outputs = mutableListOf<GameDetailOutput>()
        val interactor = GameDetailFactory(port, outputs::add).create(this, testGameId)

        interactor.dispatch(GameDetailIntent.Load)
        assertTrue(interactor.state.value.loading)
        advanceUntilIdle()

        assertFalse(interactor.state.value.loading)
        assertEquals(expected, interactor.state.value.content)
        assertEquals(listOf(testGameId), port.loads)

        interactor.dispatch(GameDetailIntent.AddClueSelected)
        interactor.dispatch(GameDetailIntent.PlaySelected(testThingId))
        interactor.dispatch(GameDetailIntent.Back)

        assertEquals(
            listOf(
                GameDetailOutput.AuthorClueRequested(testGameId),
                GameDetailOutput.PlayRequested(testGameId, testThingId),
                GameDetailOutput.Closed,
            ),
            outputs,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun local_creator_can_share_through_feature_port() = runTest {
        val expected = GameDetailContent(
            name = "Lynn Valley",
            things = emptyList(),
            localCreator = true,
        )
        val port = FakeGameDetailPort(expected, GameDetailShareResult.Shared)
        val interactor = GameDetailFactory(port, {}).create(this, testGameId)

        interactor.dispatch(GameDetailIntent.Load)
        advanceUntilIdle()
        interactor.dispatch(GameDetailIntent.ShareSelected)
        assertTrue(interactor.state.value.shareInProgress)
        advanceUntilIdle()

        assertFalse(interactor.state.value.shareInProgress)
        assertEquals(GameDetailShareResult.Shared, interactor.state.value.shareResult)
        assertEquals(listOf(testGameId to "Lynn Valley"), port.shares)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun imported_game_does_not_start_creator_actions() = runTest {
        val expected = GameDetailContent("Imported", emptyList(), localCreator = false)
        val port = FakeGameDetailPort(expected, GameDetailShareResult.Shared)
        val outputs = mutableListOf<GameDetailOutput>()
        val interactor = GameDetailFactory(port, outputs::add).create(this, testGameId)

        interactor.dispatch(GameDetailIntent.Load)
        advanceUntilIdle()
        interactor.dispatch(GameDetailIntent.ShareSelected)
        interactor.dispatch(GameDetailIntent.AddClueSelected)
        advanceUntilIdle()

        assertFalse(interactor.state.value.shareInProgress)
        assertTrue(port.shares.isEmpty())
        assertTrue(outputs.isEmpty())
    }
}

private class FakeGameDetailPort(
    private val content: GameDetailContent,
    private val shareResult: GameDetailShareResult = GameDetailShareResult.Unavailable,
) : GameDetailPort {
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
