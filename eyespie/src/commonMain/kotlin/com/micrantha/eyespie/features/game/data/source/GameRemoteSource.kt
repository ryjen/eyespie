package com.micrantha.eyespie.features.game.data.source

import com.micrantha.eyespie.core.data.client.SupaClient
import com.micrantha.eyespie.features.game.data.model.GameRemoteDetails
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
        val gameID = gameNode.onGame?.id
            ?: throw IllegalArgumentException("GraphQL game node is missing Game id")
        val things = client.gameThings(gameID).decodeList<SafeGameThingData>()
        Result.success(GameRemoteDetails(gameNode, things))
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
