package com.micrantha.eyespie.features.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun CreateGameRoute(factory: CreateGameFactory) {
    val scope = rememberCoroutineScope()
    val interactor = remember(factory, scope) { factory.create(scope) }
    val state by interactor.state.collectAsState()

    CreateGameScreen(
        state = state,
        dispatch = interactor::dispatch,
    )
}
