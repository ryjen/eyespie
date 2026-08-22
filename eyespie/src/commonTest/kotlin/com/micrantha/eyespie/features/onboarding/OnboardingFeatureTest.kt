package com.micrantha.eyespie.features.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class OnboardingFeatureTest {
    @Test
    fun reducer_moves_between_wayfinder_pages() {
        val create = OnboardingReducer.reduce(OnboardingState(), OnboardingIntent.Next)
        val share = OnboardingReducer.reduce(create, OnboardingIntent.Next)
        val join = OnboardingReducer.reduce(share, OnboardingIntent.Next)
        val back = OnboardingReducer.reduce(join, OnboardingIntent.Previous)

        assertEquals(OnboardingPage.Create, create.page)
        assertEquals(OnboardingPage.Share, share.page)
        assertEquals(OnboardingPage.Join, join.page)
        assertEquals(OnboardingPage.Share, back.page)
    }

    @Test
    fun reducer_bounds_first_and_last_pages() {
        assertEquals(
            OnboardingPage.Local,
            OnboardingReducer.reduce(OnboardingState(), OnboardingIntent.Previous).page,
        )
        assertEquals(
            OnboardingPage.Join,
            OnboardingReducer.reduce(
                OnboardingState(OnboardingPage.Join),
                OnboardingIntent.Next,
            ).page,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun done_persists_before_emitting_completion() = runTest {
        val preferences = FakeOnboardingPreferences()
        val outputs = mutableListOf<OnboardingOutput>()
        val interactor = OnboardingFactory(preferences, outputs::add).create(this)

        interactor.dispatch(OnboardingIntent.Done)
        assertTrue(interactor.state.value.completing)
        assertTrue(outputs.isEmpty())

        advanceUntilIdle()

        assertTrue(preferences.completed)
        assertEquals(listOf<OnboardingOutput>(OnboardingOutput.Completed), outputs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun skip_persists_before_semantic_dismissal() = runTest {
        val preferences = FakeOnboardingPreferences()
        val outputs = mutableListOf<OnboardingOutput>()
        val interactor = OnboardingFactory(preferences, outputs::add).create(this)

        interactor.dispatch(OnboardingIntent.Skip)
        advanceUntilIdle()

        assertTrue(preferences.completed)
        assertEquals(listOf<OnboardingOutput>(OnboardingOutput.Dismissed), outputs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun persistence_failure_stays_on_onboarding_and_allows_retry() = runTest {
        val preferences = FakeOnboardingPreferences(failWrites = true)
        val outputs = mutableListOf<OnboardingOutput>()
        val interactor = OnboardingFactory(preferences, outputs::add).create(this)

        interactor.dispatch(OnboardingIntent.Done)
        advanceUntilIdle()

        assertFalse(interactor.state.value.completing)
        assertTrue(interactor.state.value.completionFailed)
        assertTrue(outputs.isEmpty())
    }
}

private class FakeOnboardingPreferences(
    private val failWrites: Boolean = false,
) : OnboardingPreferenceStore {
    var completed = false

    override suspend fun isCompleted(): Boolean = completed

    override suspend fun markCompleted() {
        if (failWrites) error("write failed")
        completed = true
    }
}
