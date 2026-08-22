package com.micrantha.eyespie.core

interface GameRepository {
    suspend fun list(): List<Game>
    suspend fun get(id: GameId): Game?
    suspend fun save(game: Game)
}
