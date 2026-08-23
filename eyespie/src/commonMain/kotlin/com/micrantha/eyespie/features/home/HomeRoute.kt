package com.micrantha.eyespie.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.getString

@Composable
fun HomeRoute(
    factory: HomeFactory,
    onMessage: suspend (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val interactor = remember(factory, scope) { factory.create(scope) }
    val state by interactor.state.collectAsState()

    LaunchedEffect(interactor) {
        interactor.dispatch(HomeIntent.Refresh)
    }
    LaunchedEffect(interactor, onMessage) {
        interactor.effects.collect { effect ->
            when (effect) {
                is HomeEffect.ImportFinished -> homeImportMessageResource(effect.result)?.let { resource ->
                    onMessage(getString(resource))
                }
            }
        }
    }

    DisposableEffect(interactor) {
        onDispose(interactor::dispose)
    }

    HomeScreen(
        state = state,
        dispatch = interactor::dispatch,
    )
}
