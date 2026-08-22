package com.micrantha.eyespie.features.onboarding

import com.micrantha.eyespie.mvi.Reducer

object OnboardingReducer : Reducer<OnboardingState, OnboardingIntent> {
    override fun reduce(state: OnboardingState, intent: OnboardingIntent): OnboardingState = when (intent) {
        OnboardingIntent.Next -> if (state.completing) state else state.copy(
            page = when (state.page) {
                OnboardingPage.Local -> OnboardingPage.Create
                OnboardingPage.Create -> OnboardingPage.Share
                OnboardingPage.Share -> OnboardingPage.Join
                OnboardingPage.Join -> OnboardingPage.Join
            },
        )
        OnboardingIntent.Previous -> if (state.completing) state else state.copy(
            page = when (state.page) {
                OnboardingPage.Local -> OnboardingPage.Local
                OnboardingPage.Create -> OnboardingPage.Local
                OnboardingPage.Share -> OnboardingPage.Create
                OnboardingPage.Join -> OnboardingPage.Share
            },
        )
        OnboardingIntent.Skip,
        OnboardingIntent.Done -> if (state.completing) state else state.copy(completing = true)
        OnboardingIntent.Back -> OnboardingState()
    }
}
