package com.micrantha.eyespie.features.onboarding

import com.micrantha.eyespie.mvi.Interactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingInteractor(
    private val preferences: OnboardingPreferenceStore,
    private val scope: CoroutineScope,
    private val output: (OnboardingOutput) -> Unit,
    initialState: OnboardingState = OnboardingState(),
) : Interactor<OnboardingState, OnboardingIntent> {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<OnboardingState> = mutableState.asStateFlow()

    override fun dispatch(intent: OnboardingIntent) {
        val previousState = mutableState.value
        mutableState.value = OnboardingReducer.reduce(previousState, intent)
        when (intent) {
            OnboardingIntent.Done -> if (!previousState.completing) {
                persistCompletionThen(OnboardingOutput.Completed)
            }
            OnboardingIntent.Skip -> if (!previousState.completing) {
                persistCompletionThen(OnboardingOutput.Dismissed)
            }
            OnboardingIntent.Back -> output(OnboardingOutput.Dismissed)
            else -> Unit
        }
    }

    private fun persistCompletionThen(result: OnboardingOutput) {
        scope.launch {
            try {
                preferences.markCompleted()
            } finally {
                output(result)
            }
        }
    }
}
