package com.micrantha.eyespie.features.players.data.source

import com.micrantha.eyespie.features.players.data.model.PlayerResponse

internal interface PlayersLocalSource {
    fun getAll(): Result<List<PlayerResponse>>
    fun saveAll(players: List<PlayerResponse>): Result<Unit>
}
