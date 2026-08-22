package com.micrantha.eyespie.features.onboarding

import com.micrantha.eyespie.mvi.BaseInteractor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class OnboardingInteractor(
    private val preferences: OnboardingPreferenceStore,
    private val scope: CoroutineScope,
    private val output: (OnboardingOutput) -> Unit,
    initialState: OnboardingState = OnboardingState(),
) : BaseInteractor<OnboardingState, OnboardingIntent>(initialState, OnboardingReducer) {
    override fun afterReduce(
        intent: OnboardingIntent,
        previousState: OnboardingState,
        stateAfterReduce: OnboardingState,
    ) {
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
                output(result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                dispatch(OnboardingIntent.CompletionFailed)
            }
        }
    }
}
