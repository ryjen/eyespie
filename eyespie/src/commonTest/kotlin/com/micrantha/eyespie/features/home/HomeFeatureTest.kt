package com.micrantha.eyespie.features.home

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

    @Test
    fun factory_wires_semantic_outputs_without_app_routes() = runTest {
        val outputs = mutableListOf<HomeOutput>()
        val interactor = HomeFactory(FakeHomePort(HomeContent("Agent", "id", emptyList())), outputs::add)
            .create(this)

        interactor.dispatch(HomeIntent.CreateSelected)
        interactor.dispatch(HomeIntent.PlaySelected(testGameId, testThingId))

        assertEquals(
            listOf(HomeOutput.CreateRequested, HomeOutput.PlayRequested(testGameId, testThingId)),
            outputs,
        )
    }
}

private class FakeHomePort(
    private val content: HomeContent,
) : HomePort {
    var loads = 0
    override suspend fun load(): LocalGameResult<HomeContent> {
        loads += 1
        return LocalGameResult.Success(content)
    }
}
