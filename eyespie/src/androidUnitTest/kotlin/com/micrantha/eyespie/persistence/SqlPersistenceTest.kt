package com.micrantha.eyespie.persistence

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.clue.ClueAuthoringResult
import com.micrantha.eyespie.clue.GeneratedClueProvenance
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
    fun gameRepositoryRoundTripsOrderedThingsEmbeddingThresholdsAndClueAuthority() = withDatabase { database ->
        val repository = SqlGameRepository(database)
        val manualAuthority = accepted(
            ClueAuthority.manual(
                clueText = "Something striped",
                expectedAnswer = "crosswalk",
            ),
        )
        val generatedAuthority = accepted(
            ClueAuthority.generated(
                clueText = "Something round",
                expectedAnswer = "traffic sign",
                provenance = GeneratedClueProvenance(
                    providerId = "local-test-provider",
                    modelId = "test-model-v1",
                    confidence = 0.72,
                ),
            ),
        )
        val game = Game(
            id = GameId("game-1"),
            name = "Road Trip",
            creator = PlayerId("creator-1"),
            things = listOf(
                Thing(
                    id = ThingId("thing-b"),
                    clueAuthority = manualAuthority,
                    targetEmbedding = listOf(0.25f, -1.5f, 2f),
                    matchThreshold = 0.82,
                ),
                Thing(
                    id = ThingId("thing-a"),
                    clueAuthority = generatedAuthority,
                    targetEmbedding = listOf(-0.5f, 4f, 1.25f),
                    matchThreshold = 0.71,
                ),
            ),
        )

        repository.save(game)

        assertEquals(game, repository.get(game.id))
        assertEquals(listOf(game), repository.list())
        assertEquals("Something striped", repository.get(game.id)?.things?.first()?.playableClue()?.clueText)
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
        progress.save(
            ThingProgress(
                gameId = game.id,
                thingId = stale.id,
                playerId = playerId,
                matched = false,
                bestSimilarity = 0.42,
            ),
        )

        val updatedFirst = first.copy(
            clueAuthority = accepted(ClueAuthority.manual("Updated first", "first")),
        )
        games.save(game.copy(things = listOf(updatedFirst)))

        assertEquals(listOf(updatedFirst), games.get(game.id)?.things)
        assertEquals(
            ThingProgress(game.id, first.id, playerId, matched = true, bestSimilarity = 0.94),
            progress.get(game.id, first.id, playerId),
        )
        assertNull(progress.get(game.id, stale.id, playerId))
    }

    @Test
    fun migrationPreservesLegacyClueWithoutInventingAnswerOrProvenance() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            driver.execute(
                identifier = null,
                sql = """
                    CREATE TABLE GameEntity (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        creator_id TEXT NOT NULL
                    )
                """.trimIndent(),
                parameters = 0,
                binders = null,
            )
            driver.execute(
                identifier = null,
                sql = """
                    CREATE TABLE ThingEntity (
                        id TEXT NOT NULL PRIMARY KEY,
                        game_id TEXT NOT NULL,
                        clue TEXT NOT NULL,
                        target_embedding BLOB NOT NULL,
                        match_threshold REAL NOT NULL,
                        sort_order INTEGER NOT NULL,
                        FOREIGN KEY (game_id) REFERENCES GameEntity(id) ON DELETE CASCADE
                    )
                """.trimIndent(),
                parameters = 0,
                binders = null,
            )
            driver.execute(
                identifier = null,
                sql = "INSERT INTO GameEntity(id, name, creator_id) VALUES ('game-legacy', 'Trip', 'creator-1')",
                parameters = 0,
                binders = null,
            )
            driver.execute(
                identifier = null,
                sql = """
                    INSERT INTO ThingEntity(id, game_id, clue, target_embedding, match_threshold, sort_order)
                    VALUES ('thing-legacy', 'game-legacy', 'Old clue', x'00000000', 0.75, 0)
                """.trimIndent(),
                parameters = 0,
                binders = null,
            )

            EyesPieDatabase.Schema.migrate(driver, 1L, 2L)

            val migrated = EyesPieDatabase(driver).eyesPieQueries.selectThingsByGame("game-legacy") {
                    _,
                    _,
                    clue,
                    _,
                    _,
                    _,
                    expectedAnswer,
                    origin,
                    version,
                    providerId,
                    modelId,
                    confidence,
                ->
                MigratedClueRow(
                    clue = clue,
                    expectedAnswer = expectedAnswer,
                    origin = origin,
                    version = version,
                    providerId = providerId,
                    modelId = modelId,
                    confidence = confidence,
                )
            }.executeAsOne()

            assertEquals("Old clue", migrated.clue)
            assertNull(migrated.expectedAnswer)
            assertEquals("LEGACY", migrated.origin)
            assertEquals(1L, migrated.version)
            assertNull(migrated.providerId)
            assertNull(migrated.modelId)
            assertNull(migrated.confidence)
        } finally {
            driver.close()
        }
    }

    private fun accepted(result: ClueAuthoringResult): ClueAuthority = when (result) {
        is ClueAuthoringResult.Accepted -> result.authority
        is ClueAuthoringResult.Rejected -> error("expected accepted clue authority, got ${result.error}")
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

    private data class MigratedClueRow(
        val clue: String,
        val expectedAnswer: String?,
        val origin: String,
        val version: Long,
        val providerId: String?,
        val modelId: String?,
        val confidence: Double?,
    )
}
