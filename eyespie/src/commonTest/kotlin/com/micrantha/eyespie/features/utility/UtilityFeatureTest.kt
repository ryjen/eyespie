package com.micrantha.eyespie.features.utility

import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameFailureCode
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class UtilityFeatureTest {
    @Test
    fun reducer_rejects_stale_load_completion() {
        val initial = UtilityState(content = UtilityContent("Current", "one"), loading = false)
        val loading = UtilityReducer.reduce(initial, UtilityIntent.Load)
        val newer = UtilityReducer.reduce(loading, UtilityIntent.Retry)
        val stale = UtilityReducer.reduce(
            newer,
            UtilityIntent.ContentLoaded(loading.loadGeneration, UtilityContent("Stale", "two")),
        )

        assertEquals(initial.content, stale.content)
        assertTrue(stale.loading)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun factory_maps_local_identity_and_emits_semantic_navigation() = runTest {
        val loader = FakeSnapshotLoader()
        val outputs = mutableListOf<UtilityOutput>()
        val interactor = UtilityFactory(loader, outputs::add).create(this)

        interactor.dispatch(UtilityIntent.Load)
        advanceUntilIdle()

        assertEquals(UtilityContent("Agent", "player-1"), interactor.state.value.content)
        assertNull(interactor.state.value.failure)
        assertEquals(1, loader.loads)

        interactor.dispatch(UtilityIntent.OnboardingSelected)
        interactor.dispatch(UtilityIntent.Back)
        assertEquals(
            listOf(UtilityOutput.OnboardingRequested, UtilityOutput.Closed),
            outputs,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun retry_recovers_from_local_identity_load_failure() = runTest {
        val loader = FakeSnapshotLoader(failFirst = true)
        val interactor = UtilityFactory(loader, {}).create(this)

        interactor.dispatch(UtilityIntent.Load)
        advanceUntilIdle()
        assertEquals(LocalGameFailureCode.IDENTITY_UNAVAILABLE, interactor.state.value.failure?.code)

        interactor.dispatch(UtilityIntent.Retry)
        advanceUntilIdle()
        assertEquals(UtilityContent("Agent", "player-1"), interactor.state.value.content)
        assertNull(interactor.state.value.failure)
    }
}

private class FakeSnapshotLoader(
    private val failFirst: Boolean = false,
) : GameSnapshotLoader {
    var loads = 0

    override suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot> {
        loads += 1
        if (failFirst && loads == 1) {
            return LocalGameResult.Failure(LocalGameFailure(LocalGameFailureCode.IDENTITY_UNAVAILABLE))
        }
        return LocalGameResult.Success(
            LocalGameSnapshot(
                identity = PlayerIdentity(PlayerId("player-1"), "Agent"),
                games = emptyList(),
            ),
        )
    }
}
