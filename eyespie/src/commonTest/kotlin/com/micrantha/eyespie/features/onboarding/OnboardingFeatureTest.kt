package com.micrantha.eyespie.features.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingFeatureTest {
    @Test
    fun reducer_moves_between_pages() {
        val create = OnboardingReducer.reduce(OnboardingState(), OnboardingIntent.Next)
        val play = OnboardingReducer.reduce(create, OnboardingIntent.Next)
        val back = OnboardingReducer.reduce(play, OnboardingIntent.Previous)

        assertEquals(OnboardingPage.Create, create.page)
        assertEquals(OnboardingPage.Play, play.page)
        assertEquals(OnboardingPage.Create, back.page)
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
}
