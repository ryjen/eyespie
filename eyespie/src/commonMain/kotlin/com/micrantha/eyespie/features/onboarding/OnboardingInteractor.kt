package com.micrantha.eyespie.features.onboarding

import com.micrantha.eyespie.mvi.Interactor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingInteractor(
    private val output: (OnboardingOutput) -> Unit,
    initialState: OnboardingState = OnboardingState(),
) : Interactor<OnboardingState, OnboardingIntent> {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<OnboardingState> = mutableState.asStateFlow()

    override fun dispatch(intent: OnboardingIntent) {
        mutableState.value = OnboardingReducer.reduce(mutableState.value, intent)
        when (intent) {
            OnboardingIntent.Done -> output(OnboardingOutput.Completed)
            OnboardingIntent.Skip,
            OnboardingIntent.Back -> output(OnboardingOutput.Dismissed)
            else -> Unit
        }
    }
}
