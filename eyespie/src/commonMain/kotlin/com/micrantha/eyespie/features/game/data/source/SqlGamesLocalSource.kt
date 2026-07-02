package com.micrantha.eyespie.features.game.data.source

import com.micrantha.eyespie.data.EyesPieDatabase
import com.micrantha.eyespie.features.game.data.model.GameData

internal class SqlGamesLocalSource(
    database: EyesPieDatabase
) : GamesLocalSource {
    private val queries = database.eyesPieQueries

    override fun getAll(): Result<List<GameData>> = try {
        val games = queries.selectAllGames { id, title, created_at, expires_at, creator_id, player_count ->
            GameData(
                id = id,
                title = title,
                createdAt = created_at,
                expiresAt = expires_at,
                creatorId = creator_id,
                playerCount = player_count.toInt()
            )
        }.executeAsList()
        Result.success(games)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override fun saveAll(games: List<GameData>): Result<Unit> = try {
        queries.transaction {
            games.forEach { game ->
                queries.insertGame(
                    id = game.id,
                    title = game.title,
                    created_at = game.createdAt,
                    expires_at = game.expiresAt,
                    creator_id = game.creatorId,
                    player_count = game.playerCount.toLong()
                )
            }
        }
        Result.success(Unit)
    } catch (err: Throwable) {
        Result.failure(err)
    }
}
