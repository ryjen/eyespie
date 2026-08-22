package com.micrantha.eyespie.features.create

import com.micrantha.eyespie.core.GameId

sealed interface CreateGameMode {
    data object NewGame : CreateGameMode
    data class AddClue(val gameId: GameId) : CreateGameMode
}

data class CreateGameState(
    val mode: CreateGameMode = CreateGameMode.NewGame,
    val name: String = "",
    val clue: String = "",
    val expectedAnswer: String = "",
    val busy: Boolean = false,
    val failure: CreateGameFailure? = null,
)
