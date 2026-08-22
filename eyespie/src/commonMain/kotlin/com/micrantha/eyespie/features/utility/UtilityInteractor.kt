package com.micrantha.eyespie.features.utility

import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.BaseInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class UtilityInteractor(
    private val loader: UtilityLoader,
    private val scope: CoroutineScope,
    private val output: (UtilityOutput) -> Unit,
    initialState: UtilityState = UtilityState(),
) : BaseInteractor<UtilityState, UtilityIntent>(initialState, UtilityReducer) {
    override fun afterReduce(
        intent: UtilityIntent,
        previousState: UtilityState,
        stateAfterReduce: UtilityState,
    ) {
        when (intent) {
            UtilityIntent.Load,
            UtilityIntent.Retry -> {
                val generation = stateAfterReduce.loadGeneration
                scope.launch {
                    when (val result = loader.loadUtility()) {
                        is LocalGameResult.Success -> dispatch(UtilityIntent.ContentLoaded(generation, result.value))
                        is LocalGameResult.Failure -> dispatch(UtilityIntent.LoadFailed(generation, result.failure))
                    }
                }
            }
            UtilityIntent.OnboardingSelected -> output(UtilityOutput.OnboardingRequested)
            UtilityIntent.Back -> output(UtilityOutput.Closed)
            else -> Unit
        }
    }
}
