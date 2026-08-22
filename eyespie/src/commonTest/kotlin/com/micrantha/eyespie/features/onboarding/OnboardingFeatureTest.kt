package com.micrantha.eyespie.features.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun factory_wires_semantic_completion_output() {
        val outputs = mutableListOf<OnboardingOutput>()
        val interactor = OnboardingFactory(outputs::add).create()

        interactor.dispatch(OnboardingIntent.Next)
        interactor.dispatch(OnboardingIntent.Done)

        assertEquals(listOf<OnboardingOutput>(OnboardingOutput.Completed), outputs)
        assertEquals(OnboardingState(), interactor.state.value)
    }

    @Test
    fun skip_is_a_semantic_dismissal() {
        val outputs = mutableListOf<OnboardingOutput>()
        val interactor = OnboardingFactory(outputs::add).create()

        interactor.dispatch(OnboardingIntent.Next)
        interactor.dispatch(OnboardingIntent.Skip)

        assertEquals(listOf<OnboardingOutput>(OnboardingOutput.Dismissed), outputs)
        assertEquals(OnboardingState(), interactor.state.value)
    }
}
