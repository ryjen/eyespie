package com.micrantha.eyespie.features.onboarding

import com.micrantha.eyespie.mvi.Reducer

object OnboardingReducer : Reducer<OnboardingState, OnboardingIntent> {
    override fun reduce(state: OnboardingState, intent: OnboardingIntent): OnboardingState = when (intent) {
        OnboardingIntent.Next -> state.copy(
            page = when (state.page) {
                OnboardingPage.Local -> OnboardingPage.Create
                OnboardingPage.Create -> OnboardingPage.Share
                OnboardingPage.Share -> OnboardingPage.Join
                OnboardingPage.Join -> OnboardingPage.Join
            },
        )
        OnboardingIntent.Previous -> state.copy(
            page = when (state.page) {
                OnboardingPage.Local -> OnboardingPage.Local
                OnboardingPage.Create -> OnboardingPage.Local
                OnboardingPage.Share -> OnboardingPage.Create
                OnboardingPage.Join -> OnboardingPage.Share
            },
        )
        OnboardingIntent.Skip,
        OnboardingIntent.Done,
        OnboardingIntent.Back -> OnboardingState()
    }
}
