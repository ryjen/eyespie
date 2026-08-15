package com.micrantha.eyespie.persistence

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.micrantha.eyespie.core.Game
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.Thing
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.core.ThingProgress
import com.micrantha.eyespie.data.EyesPieDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlPersistenceTest {
    @Test
    fun gameRepositoryRoundTripsOrderedThingsAndEmbeddingThresholds() = withDatabase { database ->
        val repository = SqlGameRepository(database)
        val game = Game(
            id = GameId("game-1"),
            name = "Road Trip",
            creator = PlayerId("creator-1"),
            things = listOf(
                Thing(
                    id = ThingId("thing-b"),
                    clue = "Something striped",
                    targetEmbedding = listOf(0.25f, -1.5f, 2f),
                    matchThreshold = 0.82,
                ),
                Thing(
                    id = ThingId("thing-a"),
                    clue = "Something round",
                    targetEmbedding = listOf(-0.5f, 4f, 1.25f),
                    matchThreshold = 0.71,
                ),
            ),
        )

        repository.save(game)

        assertEquals(game, repository.get(game.id))
        assertEquals(listOf(game), repository.list())
    }

    @Test
    fun authoritativeSaveRemovesStaleThingsWithoutDroppingRetainedProgress() = withDatabase { database ->
        val games = SqlGameRepository(database)
        val progress = SqlThingProgressRepository(database)
        val playerId = PlayerId("player-1")
        val first = Thing(ThingId("thing-1"), "First", listOf(1f, 0f))
        val stale = Thing(ThingId("thing-2"), "Second", listOf(0f, 1f))
        val game = Game(GameId("game-1"), "Trip", PlayerId("creator-1"), listOf(first, stale))

        games.save(game)
        progress.save(
            ThingProgress(
                gameId = game.id,
                thingId = first.id,
                playerId = playerId,
                matched = true,
                bestSimilarity = 0.94,
            ),
        )

        val updatedFirst = first.copy(clue = "Updated first")
        games.save(game.copy(things = listOf(updatedFirst)))

        assertEquals(listOf(updatedFirst), games.get(game.id)?.things)
        assertEquals(
            ThingProgress(game.id, first.id, playerId, matched = true, bestSimilarity = 0.94),
            progress.get(game.id, first.id, playerId),
        )
        assertNull(progress.get(game.id, stale.id, playerId))
    }

    private fun withDatabase(block: suspend (EyesPieDatabase) -> Unit) = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            EyesPieDatabase.Schema.create(driver)
            block(EyesPieDatabase(driver))
        } finally {
            driver.close()
        }
    }
}
