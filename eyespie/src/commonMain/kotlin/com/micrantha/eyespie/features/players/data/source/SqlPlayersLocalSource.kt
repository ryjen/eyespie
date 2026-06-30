package com.micrantha.eyespie.features.players.data.source

import com.micrantha.eyespie.data.EyesPieDatabase
import com.micrantha.eyespie.features.players.data.model.PlayerResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal class SqlPlayersLocalSource(
    database: EyesPieDatabase,
    private val json: Json
) : PlayersLocalSource {
    private val queries = database.eyesPieQueries

    override fun getAll(): Result<List<PlayerResponse>> = try {
        val players = queries.selectAllPlayers { id, user_id, first_name, last_name, nick_name, total_score, created_at, email, last_location ->
            PlayerResponse(
                id = id,
                user_id = user_id,
                first_name = first_name,
                last_name = last_name,
                nick_name = nick_name,
                total_score = total_score.toInt(),
                created_at = created_at,
                email = email,
                last_location = last_location?.let { json.parseToJsonElement(it) },
                nodeId = null
            )
        }.executeAsList()
        Result.success(players)
    } catch (err: Throwable) {
        Result.failure(err)
    }

    override fun saveAll(players: List<PlayerResponse>): Result<Unit> = try {
        queries.transaction {
            players.forEach { player ->
                queries.insertPlayer(
                    id = player.id,
                    user_id = player.user_id,
                    first_name = player.first_name,
                    last_name = player.last_name,
                    nick_name = player.nick_name,
                    total_score = player.total_score.toLong(),
                    created_at = player.created_at,
                    email = player.email,
                    last_location = player.last_location?.let { json.encodeToString(JsonElement.serializer(), it) }
                )
            }
        }
        Result.success(Unit)
    } catch (err: Throwable) {
        Result.failure(err)
    }
}
