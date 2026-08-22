package com.micrantha.eyespie.features.create

import kotlinx.coroutines.CoroutineScope

class CreateGameFactory(
    private val creator: GameCreator,
    private val output: (CreateGameOutput) -> Unit,
) {
    fun create(
        scope: CoroutineScope,
        initialState: CreateGameState = CreateGameState(),
    ): CreateGameInteractor = CreateGameInteractor(
        creator = creator,
        scope = scope,
        output = output,
        initialState = initialState,
    )
}
