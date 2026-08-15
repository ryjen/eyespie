package com.micrantha.eyespie.game

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.PlayerIdentityRepository
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.data.EyesPieDatabase
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_DIMENSIONS
import com.micrantha.eyespie.imaging.ImageEmbeddingGenerator
import com.micrantha.eyespie.persistence.SqlGameRepository
import com.micrantha.eyespie.persistence.SqlThingProgressRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalGameLoopSqlTest {
    @Test
    fun sqlBackedCreateGuessAndReloadPreservesPlayableStateAndProgress() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            EyesPieDatabase.Schema.create(driver)
            val database = EyesPieDatabase(driver)
            val gameRepository = SqlGameRepository(database)
            val progressRepository = SqlThingProgressRepository(database)
            val identity = PlayerIdentity(PlayerId("player:test"), "Agent")
            val ids = FixedIds(GameId("game:test"), ThingId("thing:test"))

            val createLoop = LocalGameLoop(
                identityRepository = FixedIdentity(identity),
                gameRepository = gameRepository,
                progressRepository = progressRepository,
                embeddingGenerator = FixedEmbedding(unitVector(0)),
                idGenerator = ids,
            )

            val created = success(
                createLoop.createGame(
                    name = "Road Trip",
                    clueText = "Something striped",
                    expectedAnswer = "crosswalk",
                    targetImage = image(),
                ),
            )

            val authoritativeThing = assertNotNull(gameRepository.get(created.gameId)).things.single()
            assertEquals("crosswalk", authoritativeThing.clueAuthority.expectedAnswer)

            val reloadBeforeGuess = LocalGameLoop(
                identityRepository = FixedIdentity(identity),
                gameRepository = gameRepository,
                progressRepository = progressRepository,
                embeddingGenerator = FixedEmbedding(unitVector(0)),
                idGenerator = ids,
            )
            val beforeGuess = success(reloadBeforeGuess.loadSnapshot())
            val playableBeforeGuess = beforeGuess.games.single().things.single()
            assertEquals("Something striped", playableBeforeGuess.clue.clueText)
            assertNull(playableBeforeGuess.progress)

            val guess = success(
                reloadBeforeGuess.guess(
                    gameId = created.gameId,
                    thingId = created.thingId,
                    guessImage = image(2),
                ),
            )
            assertTrue(guess.match.matched)
            assertTrue(guess.progress.matched)
            assertEquals(1.0, guess.progress.bestSimilarity)

            val reloadAfterGuess = LocalGameLoop(
                identityRepository = FixedIdentity(identity),
                gameRepository = gameRepository,
                progressRepository = progressRepository,
                embeddingGenerator = FixedEmbedding(unitVector(1)),
                idGenerator = ids,
            )
            val restored = success(reloadAfterGuess.loadSnapshot())
                .games.single().things.single()
            assertEquals("Something striped", restored.clue.clueText)
            assertNotNull(restored.progress)
            assertTrue(restored.progress.matched)
            assertEquals(1.0, restored.progress.bestSimilarity)
        } finally {
            driver.close()
        }
    }

    private fun image(seed: Int = 1): CapturedImage =
        CapturedImage.fromEncoded(byteArrayOf(seed.toByte()))

    private fun unitVector(index: Int): List<Float> =
        List(IMAGE_EMBEDDING_DIMENSIONS) { if (it == index) 1f else 0f }

    private fun <T> success(result: LocalGameResult<T>): T = when (result) {
        is LocalGameResult.Success -> result.value
        is LocalGameResult.Failure -> error("expected success, got ${result.failure.code}")
    }
}

private class FixedIdentity(
    private val identity: PlayerIdentity,
) : PlayerIdentityRepository {
    override suspend fun current(): PlayerIdentity = identity
}

private class FixedIds(
    private val gameId: GameId,
    private val thingId: ThingId,
) : LocalGameIdGenerator {
    override fun nextGameId(): GameId = gameId
    override fun nextThingId(): ThingId = thingId
}

private class FixedEmbedding(
    private val embedding: List<Float>,
) : ImageEmbeddingGenerator {
    override suspend fun generate(image: CapturedImage): List<Float> = embedding
}
