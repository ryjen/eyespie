package com.micrantha.eyespie.features.game.data.source

import com.micrantha.bluebell.observability.logger
import com.micrantha.eyespie.core.data.client.SupaClient

class GameRemoteSource(
    private val client: SupaClient
) {

    suspend fun games() = try {
        val games = client.games().execute()
            .dataAssertNoErrors.games!!.edges!!.filterNotNull()
            .map { it.node }
        Result.success(games)
    } catch (e: Throwable) {
        Result.failure(e)
    }

    suspend fun game(id: String) = try {
        val game = with(client.game(id).execute()) {
            dataAssertNoErrors.gameNode!!
        }
        Result.success(game)
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
