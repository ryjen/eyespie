package com.micrantha.eyespie.sharing

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.micrantha.eyespie.clue.ClueAuthoringResult
import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.clue.ClueOrigin
import com.micrantha.eyespie.core.Game
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.PlayerIdentityRepository
import com.micrantha.eyespie.core.Thing
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.data.EyesPieDatabase
import com.micrantha.eyespie.game.LocalGameIdGenerator
import com.micrantha.eyespie.game.LocalGameLoop
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.identity.SigningIdentity
import com.micrantha.eyespie.identity.playerIdFor
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_FILE
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_ID
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_SHA256
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_DIMENSIONS
import com.micrantha.eyespie.imaging.ImageEmbeddingGenerator
import com.micrantha.eyespie.persistence.SqlGameRepository
import com.micrantha.eyespie.persistence.SqlThingProgressRepository
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameBundleIntegrationTest {
    @Test
    fun applicationEmbeddingConstantsMatchReviewedManifest() {
        val manifest = locateModelManifest().readText()

        assertTrue(manifest.contains("\"model_id\": \"$IMAGE_EMBEDDER_MODEL_ID\""))
        assertTrue(manifest.contains("\"file_name\": \"$IMAGE_EMBEDDER_MODEL_FILE\""))
        assertTrue(manifest.contains("\"sha256\": \"$IMAGE_EMBEDDER_MODEL_SHA256\""))
        assertTrue(manifest.contains("\"embedding_dimension\": $IMAGE_EMBEDDING_DIMENSIONS"))
    }

    @Test
    fun canonicalGoldenSignedFileHasStableSha256() {
        val publicKey = integrationPublicKey(61)
        val portable = PortableGame(
            gameId = "game:golden",
            gameName = "Golden Trip",
            creatorPlayerId = playerIdFor(publicKey).value,
            creatorPublicKey = publicKey,
            things = listOf(
                PortableThing(
                    thingId = "thing:golden",
                    clueText = "Find the golden target",
                    targetEmbedding = List(IMAGE_EMBEDDING_DIMENSIONS) { if (it == 0) 1f else 0f },
                    matchThreshold = 0.75,
                ),
            ),
        )
        val codec = GameBundleCodec()
        val unsigned = codec.encodeUnsigned(portable)
        val signature = deterministicDerFixtureSignature(publicKey, unsigned)
        val signed = codec.encodeSigned(unsigned, signature)

        assertEquals(
            "d1e0db592801eaa876ce768010b355cbb498c6f77ef947acf166bdb799ec18c9",
            sha256Hex(signed),
        )
        val decoded = assertIs<GameBundleDecodeResult.Success>(codec.decode(signed)).bundle
        assertTrue(portable.equivalentTo(decoded.game))
    }

    @Test
    fun signedImportPersistsSharedAuthorityAndReloadsThroughLocalGameLoop() = runTest {
        val sourceDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val destinationDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            EyesPieDatabase.Schema.create(sourceDriver)
            EyesPieDatabase.Schema.create(destinationDriver)
            val sourceDatabase = EyesPieDatabase(sourceDriver)
            val destinationDatabase = EyesPieDatabase(destinationDriver)

            val sourceKey = integrationPublicKey(67)
            val sourceIdentity = PlayerIdentity(playerIdFor(sourceKey), "Creator")
            val sourceGames = SqlGameRepository(sourceDatabase)
            sourceGames.save(authoredGame(sourceIdentity.id))
            val sourceService = GameBundleService(
                identityRepository = FixedIntegrationIdentity(sourceIdentity),
                signingIdentity = IntegrationSigningIdentity(sourceKey),
                gameRepository = sourceGames,
            )
            val bundle = assertIs<GameBundleExportResult.Success>(
                sourceService.export(GameId("game:sql")),
            ).bytes

            val destinationKey = integrationPublicKey(71)
            val destinationIdentity = PlayerIdentity(playerIdFor(destinationKey), "Player")
            val destinationGames = SqlGameRepository(destinationDatabase)
            val destinationProgress = SqlThingProgressRepository(destinationDatabase)
            val destinationService = GameBundleService(
                identityRepository = FixedIntegrationIdentity(destinationIdentity),
                signingIdentity = IntegrationSigningIdentity(destinationKey),
                gameRepository = destinationGames,
            )

            assertEquals(
                GameBundleImportResult.Imported(GameId("game:sql")),
                destinationService.import(bundle),
            )

            val imported = assertNotNull(destinationGames.get(GameId("game:sql")))
            val authority = imported.things.single().clueAuthority
            assertEquals(ClueOrigin.SHARED, authority.origin)
            assertEquals("Find the striped crossing", authority.clueText)
            assertNull(authority.expectedAnswer)
            assertNull(authority.generatedProvenance)

            val reloadedLoop = LocalGameLoop(
                identityRepository = FixedIntegrationIdentity(destinationIdentity),
                gameRepository = destinationGames,
                progressRepository = destinationProgress,
                embeddingGenerator = FixedIntegrationEmbedding(),
                idGenerator = FixedIntegrationIds,
            )
            val snapshot = assertIs<LocalGameResult.Success<*>>(reloadedLoop.loadSnapshot()).value
                as com.micrantha.eyespie.game.LocalGameSnapshot
            val playable = snapshot.games.single().things.single()
            assertEquals("Find the striped crossing", playable.clue.clueText)
            assertNull(playable.progress)

            assertEquals(
                GameBundleImportResult.AlreadyPresent(GameId("game:sql")),
                destinationService.import(bundle),
            )
        } finally {
            sourceDriver.close()
            destinationDriver.close()
        }
    }

    private fun locateModelManifest(): File {
        val candidates = listOf(
            File("models/image-embedder.json"),
            File("../models/image-embedder.json"),
            File(System.getProperty("user.dir"), "models/image-embedder.json"),
            File(System.getProperty("user.dir"), "../models/image-embedder.json"),
        )
        return assertNotNull(candidates.firstOrNull(File::isFile), "unable to locate models/image-embedder.json")
    }
}

private fun authoredGame(creator: PlayerId): Game = Game(
    id = GameId("game:sql"),
    name = "SQL Trip",
    creator = creator,
    things = listOf(
        Thing(
            id = ThingId("thing:sql"),
            clueAuthority = acceptedIntegrationClue(
                ClueAuthority.manual(
                    clueText = "Find the striped crossing",
                    expectedAnswer = "crosswalk",
                ),
            ),
            targetEmbedding = List(IMAGE_EMBEDDING_DIMENSIONS) { if (it == 0) 1f else 0f },
            matchThreshold = 0.75,
        ),
    ),
)

private fun acceptedIntegrationClue(result: ClueAuthoringResult): ClueAuthority = when (result) {
    is ClueAuthoringResult.Accepted -> result.authority
    is ClueAuthoringResult.Rejected -> error("expected valid clue")
}

private class FixedIntegrationIdentity(
    private val identity: PlayerIdentity,
) : PlayerIdentityRepository {
    override suspend fun current(): PlayerIdentity = identity
}

private class IntegrationSigningIdentity(
    private val localPublicKey: ByteArray,
) : SigningIdentity {
    override suspend fun publicKey(): ByteArray = localPublicKey.copyOf()

    override suspend fun sign(payload: ByteArray): ByteArray =
        deterministicDerFixtureSignature(localPublicKey, payload)

    override suspend fun verify(
        publicKey: ByteArray,
        payload: ByteArray,
        signature: ByteArray,
    ): Boolean = signature.contentEquals(deterministicDerFixtureSignature(publicKey, payload))
}

private class FixedIntegrationEmbedding : ImageEmbeddingGenerator {
    override suspend fun generate(image: CapturedImage): List<Float> =
        List(IMAGE_EMBEDDING_DIMENSIONS) { if (it == 0) 1f else 0f }
}

private object FixedIntegrationIds : LocalGameIdGenerator {
    override fun nextGameId(): GameId = GameId("unused:game")
    override fun nextThingId(): ThingId = ThingId("unused:thing")
}

private fun integrationPublicKey(seed: Int): ByteArray = ByteArray(65) { index ->
    if (index == 0) 0x04 else ((seed + index * 17) and 0xff).toByte()
}

private fun deterministicDerFixtureSignature(publicKey: ByteArray, payload: ByteArray): ByteArray {
    val state = ByteArray(32)
    publicKey.forEachIndexed { index, byte ->
        state[index % state.size] = (state[index % state.size].toInt() xor byte.toInt()).toByte()
    }
    payload.forEachIndexed { index, byte ->
        val slot = index % state.size
        state[slot] = (state[slot].toInt() xor byte.toInt() xor (index and 0xff)).toByte()
    }
    val r = state.copyOf().also { it[0] = (it[0].toInt() or 0x80).toByte() }
    val s = state.reversedArray().also { it[0] = (it[0].toInt() or 0x80).toByte() }
    return byteArrayOf(0x30, 0x46, 0x02, 0x21, 0x00) +
        r +
        byteArrayOf(0x02, 0x21, 0x00) +
        s
}

private fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString(separator = "") { "%02x".format(it.toInt() and 0xff) }
