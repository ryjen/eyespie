package com.micrantha.eyespie.features.onboarding

class OnboardingFactory(
    private val output: (OnboardingOutput) -> Unit,
) {
    fun create(
        initialState: OnboardingState = OnboardingState(),
    ): OnboardingInteractor = OnboardingInteractor(
        output = output,
        initialState = initialState,
    )
}
