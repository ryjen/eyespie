package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.BaseInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class HomeInteractor(
    private val port: HomePort,
    private val scope: CoroutineScope,
    private val output: (HomeOutput) -> Unit,
    initialState: HomeState = HomeState(),
) : BaseInteractor<HomeState, HomeIntent>(initialState, HomeReducer) {
    override fun afterReduce(
        intent: HomeIntent,
        previousState: HomeState,
        stateAfterReduce: HomeState,
    ) {
        when (intent) {
            HomeIntent.Refresh -> {
                val generation = stateAfterReduce.refreshGeneration
                scope.launch {
                    when (val result = port.load()) {
                        is LocalGameResult.Success -> dispatch(HomeIntent.ContentLoaded(generation, result.value))
                        is LocalGameResult.Failure -> dispatch(HomeIntent.OperationFailed(result.failure, generation))
                    }
                }
            }
            HomeIntent.ImportSelected -> if (!previousState.importInProgress && previousState.importPreview == null) {
                scope.launch {
                    when (val result = port.prepareImport()) {
                        is HomeImportPreparationResult.Ready -> dispatch(HomeIntent.ImportPreviewReady(result.preview))
                        is HomeImportPreparationResult.Terminal -> dispatch(HomeIntent.ImportFinished(result.result))
                    }
                }
            }
            HomeIntent.ImportConfirmed -> if (!previousState.importInProgress && previousState.importPreview != null) {
                scope.launch {
                    val result = port.confirmImport()
                    dispatch(HomeIntent.ImportFinished(result))
                    if (result == HomeImportResult.Imported || result == HomeImportResult.AlreadyPresent) {
                        dispatch(HomeIntent.Refresh)
                    }
                }
            }
            HomeIntent.ImportPreviewCancelled -> if (!previousState.importInProgress && previousState.importPreview != null) {
                port.cancelImport()
            }
            HomeIntent.OnboardingSelected -> output(HomeOutput.OnboardingRequested)
            HomeIntent.UtilitySelected -> output(HomeOutput.UtilityRequested)
            HomeIntent.CreateSelected -> output(HomeOutput.CreateRequested)
            is HomeIntent.GameSelected -> output(HomeOutput.GameRequested(intent.gameId))
            else -> Unit
        }
    }

    fun dispose() {
        port.cancelImport()
    }
}
