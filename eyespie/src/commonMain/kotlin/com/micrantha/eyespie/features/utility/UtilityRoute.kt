package com.micrantha.eyespie.features.utility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun UtilityRoute(factory: UtilityFactory) {
    val scope = rememberCoroutineScope()
    val interactor = remember(factory, scope) { factory.create(scope) }
    val state by interactor.state.collectAsState()

    LaunchedEffect(interactor) {
        interactor.dispatch(UtilityIntent.Load)
    }

    UtilityScreen(
        state = state,
        dispatch = interactor::dispatch,
    )
}
