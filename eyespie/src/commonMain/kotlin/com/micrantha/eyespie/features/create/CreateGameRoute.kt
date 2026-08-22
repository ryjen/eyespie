package com.micrantha.eyespie.features.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.micrantha.eyespie.core.GameId

@Composable
fun CreateGameRoute(
    factory: CreateGameFactory,
    gameId: GameId? = null,
) {
    val scope = rememberCoroutineScope()
    val initialState = remember(gameId) {
        CreateGameState(
            mode = gameId?.let { CreateGameMode.AddClue(it) } ?: CreateGameMode.NewGame,
        )
    }
    val interactor = remember(factory, scope, gameId) { factory.create(scope, initialState) }
    val state by interactor.state.collectAsState()

    CreateGameScreen(
        state = state,
        dispatch = interactor::dispatch,
    )
}
