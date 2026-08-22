package com.micrantha.eyespie.features.onboarding

import kotlinx.coroutines.CoroutineScope

class OnboardingFactory(
    private val preferences: OnboardingPreferenceStore,
    private val output: (OnboardingOutput) -> Unit,
) {
    fun create(
        scope: CoroutineScope,
        initialState: OnboardingState = OnboardingState(),
    ): OnboardingInteractor = OnboardingInteractor(
        preferences = preferences,
        scope = scope,
        output = output,
        initialState = initialState,
    )
}
