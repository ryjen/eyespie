package com.micrantha.eyespie.game

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.PlayerIdentityRepository
import com.micrantha.eyespie.data.EyesPieDatabase
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.imaging.ImageEmbeddingGenerator
import com.micrantha.eyespie.persistence.SqlGameRepository
import com.micrantha.eyespie.persistence.SqlThingProgressRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OfflineGameCoordinatorSqlTest {
    @Test
    fun createGuessAndRelaunchRoundTripThroughSqlDelight() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            EyesPieDatabase.Schema.create(driver)
            val database = EyesPieDatabase(driver)
            val games = SqlGameRepository(database)
            val progress = SqlThingProgressRepository(database)
            val ids = ArrayDeque(listOf("game-1", "thing-1"))
            val creator = OfflineGameCoordinator(
                identityRepository = Identity,
                gameRepository = games,
                progressRepository = progress,
                embeddingGenerator = QueueEmbeddingGenerator(axis(0)),
                idGenerator = { ids.removeFirst() },
            )

            val created = assertIs<OfflineResult.Success<PlayableGameState>>(
                creator.createManualGame(
                    ManualGameDraft("Road Trip", "Something striped", "crosswalk"),
                    image(),
                ),
            ).value
            val persistedThing = games.get(created.id)!!.things.single()
            assertEquals(axis(0), persistedThing.targetEmbedding)
            assertEquals("crosswalk", persistedThing.clueAuthority.expectedAnswer)
            assertFalse(created.toString().contains("crosswalk"))

            val player = OfflineGameCoordinator(
                identityRepository = Identity,
                gameRepository = games,
                progressRepository = progress,
                embeddingGenerator = QueueEmbeddingGenerator(axis(0)),
                idGenerator = { error("relaunch play must not allocate ids") },
            )
            val restoredBeforeGuess = assertIs<OfflineResult.Success<List<PlayableGameState>>>(
                player.loadGames(),
            ).value.single()
            assertEquals("Something striped", restoredBeforeGuess.things.single().clue.clueText)
            assertFalse(restoredBeforeGuess.toString().contains("crosswalk"))

            val outcome = assertIs<OfflineResult.Success<GuessOutcome>>(
                player.guess(
                    gameId = restoredBeforeGuess.id,
                    thingId = restoredBeforeGuess.things.single().id,
                    guessImage = image(),
                ),
            ).value
            assertTrue(outcome.matched)
            assertEquals(1.0, outcome.similarity, 0.000001)

            val relaunched = OfflineGameCoordinator(
                identityRepository = Identity,
                gameRepository = games,
                progressRepository = progress,
                embeddingGenerator = QueueEmbeddingGenerator(axis(1)),
                idGenerator = { error("reload must not allocate ids") },
            )
            val restoredAfterGuess = assertIs<OfflineResult.Success<List<PlayableGameState>>>(
                relaunched.loadGames(),
            ).value.single()
            assertTrue(restoredAfterGuess.things.single().matched)
            assertEquals(1.0, restoredAfterGuess.things.single().bestSimilarity)
        } finally {
            driver.close()
        }
    }

    private fun axis(index: Int): List<Float> = List(1024) { if (it == index) 1f else 0f }

    private fun image(): CapturedImage = CapturedImage.fromEncoded(byteArrayOf(1, 2, 3))

    private object Identity : PlayerIdentityRepository {
        override suspend fun current(): PlayerIdentity = PlayerIdentity(
            id = PlayerId("player-1"),
            displayName = "Agent",
        )
    }

    private class QueueEmbeddingGenerator(vararg embeddings: List<Float>) : ImageEmbeddingGenerator {
        private val queue = ArrayDeque(embeddings.toList())

        override suspend fun generate(image: CapturedImage): List<Float> = queue.removeFirst()
    }
}
