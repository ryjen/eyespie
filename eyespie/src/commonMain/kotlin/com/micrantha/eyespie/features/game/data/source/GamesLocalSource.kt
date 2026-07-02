package com.micrantha.eyespie.features.game.data.source

import com.micrantha.eyespie.features.game.data.model.GameData

internal interface GamesLocalSource {
    fun getAll(): Result<List<GameData>>
    fun saveAll(games: List<GameData>): Result<Unit>
}
