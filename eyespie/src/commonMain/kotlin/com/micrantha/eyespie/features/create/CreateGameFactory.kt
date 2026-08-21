package com.micrantha.eyespie.features.create

import kotlinx.coroutines.CoroutineScope

class CreateGameFactory(
    private val port: CreateGamePort,
    private val output: (CreateGameOutput) -> Unit,
) {
    fun create(
        scope: CoroutineScope,
        initialState: CreateGameState = CreateGameState(),
    ): CreateGameInteractor = CreateGameInteractor(
        port = port,
        scope = scope,
        output = output,
        initialState = initialState,
    )
}
