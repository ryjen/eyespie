package com.micrantha.eyespie.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun HomeRoute(factory: HomeFactory) {
    val scope = rememberCoroutineScope()
    val interactor = remember(factory, scope) { factory.create(scope) }
    val state by interactor.state.collectAsState()

    LaunchedEffect(interactor) {
        interactor.dispatch(HomeIntent.Refresh)
    }

    DisposableEffect(interactor) {
        onDispose(interactor::dispose)
    }

    HomeScreen(
        state = state,
        dispatch = interactor::dispatch,
    )
}
