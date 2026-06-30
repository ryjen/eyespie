package com.micrantha.eyespie.features.game.data.source

import com.micrantha.eyespie.core.data.client.SupaClient
import com.micrantha.eyespie.graphql.GameListQuery
import com.micrantha.eyespie.graphql.GameNodeQuery

internal interface GameRemoteSource {
    suspend fun games(): Result<List<GameListQuery.Node>>
    suspend fun game(id: String): Result<GameNodeQuery.GameNode>
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
        val game = with(client.game(id).execute()) {
            dataAssertNoErrors.gameNode!!
        }
        Result.success(game)
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
