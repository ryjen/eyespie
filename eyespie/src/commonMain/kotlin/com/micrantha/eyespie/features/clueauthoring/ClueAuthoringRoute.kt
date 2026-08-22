package com.micrantha.eyespie.features.clueauthoring

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.micrantha.eyespie.core.GameId

@Composable
fun ClueAuthoringRoute(
    factory: ClueAuthoringFactory,
    gameId: GameId,
) {
    val scope = rememberCoroutineScope()
    val interactor = remember(factory, scope, gameId) { factory.create(scope, gameId) }
    val state by interactor.state.collectAsState()

    ClueAuthoringScreen(
        state = state,
        dispatch = interactor::dispatch,
    )
}
