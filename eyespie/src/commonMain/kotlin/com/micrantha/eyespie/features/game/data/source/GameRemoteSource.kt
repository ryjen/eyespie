package com.micrantha.eyespie.features.game.data.source

import com.micrantha.eyespie.core.data.client.SupaClient
import com.micrantha.eyespie.features.game.data.model.GameRemoteDetails
import com.micrantha.eyespie.features.game.data.model.SafeGamePlayerData
import com.micrantha.eyespie.features.game.data.model.SafeGameThingData
import com.micrantha.eyespie.graphql.GameListQuery

internal interface GameRemoteSource {
    suspend fun games(): Result<List<GameListQuery.Node>>
    suspend fun game(id: String): Result<GameRemoteDetails>
}

internal class SupabaseGameRemoteSource(
    private val client: SupaClient
) : GameRemoteSource {

    override suspend fun games() = try {
        val games = client.games().execute()
            .dataAssertNoErrors.games!!.edges!!.filterNotNull()
            .map { it.node }
        Result.success(games)
    } catch (e: Throwable) {
        Result.failure(e)
    }

    override suspend fun game(id: String) = try {
        val gameNode = with(client.game(id).execute()) {
            dataAssertNoErrors.gameNode!!
        }
        val game = gameNode.onGame
            ?: throw IllegalArgumentException("GraphQL game node is missing Game data")
        val things = client.gameThings(game.id).decodeList<SafeGameThingData>()
        val players = game.players?.edges.orEmpty().filterNotNull().map { edge ->
            val player = edge.node.player
            SafeGamePlayerData(
                id = player.id,
                nodeId = player.nodeId,
                createdAt = player.created_at,
                firstName = player.first_name,
                lastName = player.last_name,
                score = edge.node.score ?: 0,
            )
        }
        Result.success(
            GameRemoteDetails(
                id = game.id,
                name = game.name,
                createdAt = game.created_at,
                expiresAt = game.expires,
                turnDuration = game.turn_duration,
                minThings = game.min_things,
                maxThings = game.max_things,
                minPlayers = game.min_players,
                maxPlayers = game.max_players,
                players = players,
                things = things,
            )
        )
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
