package com.micrantha.eyespie.features.guess.ui

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.ui.components.Router
import com.micrantha.bluebell.ui.components.message.popup
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.bluebell.ui.screen.ScreenEnvironment
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.features.guess.ui.ScanGuessAction.ImageCaptured
import com.micrantha.eyespie.features.guess.ui.ScanGuessAction.Load
import com.micrantha.eyespie.features.guess.ui.ScanGuessAction.Loaded
import com.micrantha.eyespie.features.guess.ui.ScanGuessAction.SimilarityUpdated
import com.micrantha.eyespie.features.guess.ui.ScanGuessAction.ThingMatched
import com.micrantha.eyespie.features.guess.ui.ScanGuessAction.ThingNotFound
import com.micrantha.eyespie.features.scan.usecase.MatchCaptureUseCase
import com.micrantha.eyespie.generated.resources.no_data_found
import com.micrantha.eyespie.generated.resources.ok
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ScanGuessEnvironment(
    private val args: ScanGuessArgs,
    private val context: ScreenContext,
    private val matchCaptureUseCase: MatchCaptureUseCase,
) : ScreenEnvironment<ScanGuessState>,
    Dispatcher by context.dispatcher,
    Router by context.router {

    override fun reduce(state: ScanGuessState, action: Action) = when (action) {
        is Loaded -> state.copy(thingID = action.thingID)
        is ThingMatched -> state.copy(guessed = true)
        is SimilarityUpdated -> state.copy(bestSimilarity = action.similarity)
        else -> state
    }

    override suspend fun invoke(action: Action, state: ScanGuessState) {
        when (action) {
            is Load -> if (args.id.isNotBlank()) {
                dispatch(Loaded(args.id))
            } else {
                dispatch(context.popup(S.no_data_found, S.ok) {
                    navigateBack()
                })
            }

            is ImageCaptured -> state.thingID?.let { thingID ->
                matchCaptureUseCase(
                    action.image,
                    thingID,
                ).onEach { res ->
                    res.onSuccess { result ->
                        if (result.matched) {
                            dispatch(ThingMatched)
                            navigateBack()
                        } else {
                            dispatch(SimilarityUpdated(result.bestSimilarity))
                            dispatch(ThingNotFound)
                        }
                    }.onFailure {
                        dispatch(ThingNotFound)
                    }
                }.launchIn(dispatchScope)
            }
        }
    }
}
