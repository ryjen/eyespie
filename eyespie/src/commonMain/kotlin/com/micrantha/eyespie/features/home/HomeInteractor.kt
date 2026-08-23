package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.BaseInteractor
import com.micrantha.eyespie.mvi.EffectEmitter
import com.micrantha.eyespie.mvi.EffectSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class HomeInteractor(
    private val snapshotLoader: GameSnapshotLoader,
    private val importPreparer: GameImportPreparer,
    private val importConfirmer: GameImportConfirmer,
    private val importCanceller: GameImportCanceller,
    private val scope: CoroutineScope,
    private val output: (HomeOutput) -> Unit,
    initialState: HomeState = HomeState(),
) : BaseInteractor<HomeState, HomeIntent>(initialState, HomeReducer), EffectSource<HomeEffect> {
    private val effectEmitter = EffectEmitter<HomeEffect>()
    override val effects: Flow<HomeEffect> = effectEmitter.effects

    override fun afterReduce(
        intent: HomeIntent,
        previousState: HomeState,
        stateAfterReduce: HomeState,
    ) {
        when (intent) {
            HomeIntent.Refresh -> {
                val generation = stateAfterReduce.refreshGeneration
                scope.launch {
                    when (val result = snapshotLoader.loadSnapshot()) {
                        is LocalGameResult.Success -> dispatch(
                            HomeIntent.ContentLoaded(generation, HomeMapper.map(result.value)),
                        )
                        is LocalGameResult.Failure -> dispatch(HomeIntent.OperationFailed(result.failure, generation))
                    }
                }
            }
            HomeIntent.ImportSelected -> if (!previousState.importInProgress && previousState.importPreview == null) {
                scope.launch {
                    when (val result = importPreparer.prepareImport()) {
                        is HomeImportPreparationResult.Ready -> dispatch(HomeIntent.ImportPreviewReady(result.preview))
                        is HomeImportPreparationResult.Terminal -> dispatch(HomeIntent.ImportFinished(result.result))
                    }
                }
            }
            HomeIntent.ImportConfirmed -> if (!previousState.importInProgress && previousState.importPreview != null) {
                scope.launch {
                    val result = importConfirmer.confirmImport()
                    dispatch(HomeIntent.ImportFinished(result))
                    if (result == HomeImportResult.Imported || result == HomeImportResult.AlreadyPresent) {
                        dispatch(HomeIntent.Refresh)
                    }
                }
            }
            is HomeIntent.ImportFinished -> if (intent.result != HomeImportResult.Cancelled) {
                effectEmitter.emit(HomeEffect.ImportFinished(intent.result))
            }
            HomeIntent.ImportPreviewCancelled -> if (!previousState.importInProgress && previousState.importPreview != null) {
                importCanceller.cancelImport()
            }
            HomeIntent.OnboardingSelected -> output(HomeOutput.OnboardingRequested)
            HomeIntent.UtilitySelected -> output(HomeOutput.UtilityRequested)
            HomeIntent.CreateSelected -> output(HomeOutput.CreateRequested)
            is HomeIntent.GameSelected -> output(HomeOutput.GameRequested(intent.gameId))
            else -> Unit
        }
    }

    fun dispose() {
        importCanceller.cancelImport()
    }
}
