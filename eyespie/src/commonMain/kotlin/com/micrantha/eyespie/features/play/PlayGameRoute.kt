package com.micrantha.eyespie.features.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId

@Composable
fun PlayGameRoute(
    factory: PlayGameFactory,
    gameId: GameId,
    thingId: ThingId,
) {
    val scope = rememberCoroutineScope()
    val interactor = remember(factory, scope, gameId, thingId) {
        factory.create(scope, gameId, thingId)
    }
    val state by interactor.state.collectAsState()

    LaunchedEffect(interactor) {
        interactor.dispatch(PlayGameIntent.Load)
    }

    PlayGameScreen(
        state = state,
        dispatch = interactor::dispatch,
    )
}
