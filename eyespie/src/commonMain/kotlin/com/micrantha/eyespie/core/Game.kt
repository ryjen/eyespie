package com.micrantha.eyespie.core

data class Game(
    val id: GameId,
    val name: String,
    val creator: PlayerId,
    val things: List<Thing> = emptyList(),
)
