package com.micrantha.eyespie.features.onboarding

import com.micrantha.eyespie.mvi.Reducer

object OnboardingReducer : Reducer<OnboardingState, OnboardingIntent> {
    override fun reduce(state: OnboardingState, intent: OnboardingIntent): OnboardingState = when (intent) {
        OnboardingIntent.Next -> state.copy(
            page = when (state.page) {
                OnboardingPage.Welcome -> OnboardingPage.Create
                OnboardingPage.Create -> OnboardingPage.Play
                OnboardingPage.Play -> OnboardingPage.Play
            },
        )
        OnboardingIntent.Previous -> state.copy(
            page = when (state.page) {
                OnboardingPage.Welcome -> OnboardingPage.Welcome
                OnboardingPage.Create -> OnboardingPage.Welcome
                OnboardingPage.Play -> OnboardingPage.Create
            },
        )
        OnboardingIntent.Done,
        OnboardingIntent.Back -> OnboardingState()
    }
}
