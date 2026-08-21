package com.micrantha.eyespie.features.home

import kotlinx.coroutines.CoroutineScope

class HomeFactory(
    private val port: HomePort,
    private val output: (HomeOutput) -> Unit,
) {
    fun create(
        scope: CoroutineScope,
        initialState: HomeState = HomeState(),
    ): HomeInteractor = HomeInteractor(
        port = port,
        scope = scope,
        output = output,
        initialState = initialState,
    )
}
