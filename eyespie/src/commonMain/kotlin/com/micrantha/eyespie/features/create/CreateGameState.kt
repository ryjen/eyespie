package com.micrantha.eyespie.features.create

data class CreateGameState(
    val name: String = "",
    val clue: String = "",
    val expectedAnswer: String = "",
    val busy: Boolean = false,
    val failure: CreateGameFailure? = null,
)
