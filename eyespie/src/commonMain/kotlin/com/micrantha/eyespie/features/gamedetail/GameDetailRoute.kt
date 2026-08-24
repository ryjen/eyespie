package com.micrantha.eyespie.features.gamedetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.micrantha.eyespie.core.GameId
import org.jetbrains.compose.resources.getString

@Composable
fun GameDetailRoute(
    factory: GameDetailFactory,
    gameId: GameId,
    onMessage: suspend (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val interactor = remember(factory, scope, gameId) {
        factory.create(scope, gameId)
    }
    val state by interactor.state.collectAsState()

    LaunchedEffect(interactor) {
        interactor.dispatch(GameDetailIntent.Load)
    }
    LaunchedEffect(interactor, onMessage) {
        interactor.effects.collect { effect ->
            when (effect) {
                is GameDetailEffect.ShareFinished -> gameDetailShareMessageResource(effect.result)?.let { resource ->
                    onMessage(getString(resource))
                }
            }
        }
    }

    GameDetailScreen(
        state = state,
        dispatch = interactor::dispatch,
    )
}
