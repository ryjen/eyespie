package com.micrantha.eyespie.features.clueauthoring

data class ClueAuthoringState(
    val clue: String = "",
    val expectedAnswer: String = "",
    val busy: Boolean = false,
    val failure: ClueAuthoringFailure? = null,
)
