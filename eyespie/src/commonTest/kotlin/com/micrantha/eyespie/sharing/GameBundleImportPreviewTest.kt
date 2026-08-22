package com.micrantha.eyespie.sharing

import com.micrantha.eyespie.core.Game
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.GameRepository
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.PlayerIdentityRepository
import com.micrantha.eyespie.identity.SigningIdentity
import com.micrantha.eyespie.identity.playerIdFor
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_DIMENSIONS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class GameBundleImportPreviewTest {
    @Test
    fun verified_preview_exposes_bounded_metadata_without_persisting() = runTest {
        val creatorKey = testPublicKey(71)
        val creator = PlayerIdentity(playerIdFor(creatorKey), "Creator")
        val signing = PreviewSigningIdentity(creatorKey)
        val repository = PreviewGameRepository()
        val service = GameBundleService(
            identityRepository = PreviewIdentityRepository(creator),
            signingIdentity = signing,
            gameRepository = repository,
        )
        val bytes = signedPreviewBundle(creatorKey, signing)

        val ready = assertIs<GameBundleImportPreviewResult.Ready>(service.previewImport(bytes))

        assertEquals(GameId("game:preview"), ready.preview.gameId)
        assertEquals("Road Trip", ready.preview.gameName)
        assertEquals(creator.id, ready.preview.creatorPlayerId)
        assertEquals(1, ready.preview.thingCount)
        assertEquals(0, repository.saves)
        assertNull(repository.get(GameId("game:preview")))
    }

    @Test
    fun invalid_signature_fails_preview_before_persistence() = runTest {
        val creatorKey = testPublicKey(73)
        val creator = PlayerIdentity(playerIdFor(creatorKey), "Creator")
        val signing = PreviewSigningIdentity(creatorKey)
        val repository = PreviewGameRepository()
        val service = GameBundleService(
            identityRepository = PreviewIdentityRepository(creator),
            signingIdentity = signing,
            gameRepository = repository,
        )
        val bytes = signedPreviewBundle(creatorKey, signing).copyOf().also {
            it[it.lastIndex] = (it.last().toInt() xor 0x01).toByte()
        }

        val failure = assertIs<GameBundleImportPreviewResult.Failure>(service.previewImport(bytes))

        assertEquals(GameBundleImportFailureCode.INVALID_SIGNATURE, failure.code)
        assertEquals(0, repository.saves)
    }
}

private suspend fun signedPreviewBundle(
    creatorKey: ByteArray,
    signing: SigningIdentity,
): ByteArray {
    val codec = GameBundleCodec()
    val portable = PortableGame(
        gameId = "game:preview",
        gameName = "Road Trip",
        creatorPlayerId = playerIdFor(creatorKey).value,
        creatorPublicKey = creatorKey,
        things = listOf(
            PortableThing(
                thingId = "thing:preview",
                clueText = "Find something striped",
                targetEmbedding = List(IMAGE_EMBEDDING_DIMENSIONS) { if (it == 0) 1f else 0f },
                matchThreshold = 0.75,
            ),
        ),
    )
    val unsigned = codec.encodeUnsigned(portable)
    return codec.encodeSigned(unsigned, signing.sign(unsigned))
}

private class PreviewIdentityRepository(
    private val identity: PlayerIdentity,
) : PlayerIdentityRepository {
    override suspend fun current(): PlayerIdentity = identity
}

private class PreviewSigningIdentity(
    private val localPublicKey: ByteArray,
) : SigningIdentity {
    override suspend fun publicKey(): ByteArray = localPublicKey.copyOf()

    override suspend fun sign(payload: ByteArray): ByteArray = signatureFor(localPublicKey, payload)

    override suspend fun verify(
        publicKey: ByteArray,
        payload: ByteArray,
        signature: ByteArray,
    ): Boolean = signature.contentEquals(signatureFor(publicKey, payload))

    private fun signatureFor(publicKey: ByteArray, payload: ByteArray): ByteArray {
        val state = ByteArray(32)
        publicKey.forEachIndexed { index, byte ->
            state[index % state.size] = (state[index % state.size].toInt() xor byte.toInt()).toByte()
        }
        payload.forEachIndexed { index, byte ->
            val slot = index % state.size
            state[slot] = (state[slot].toInt() xor byte.toInt() xor (index and 0xff)).toByte()
        }
        return state
    }
}

private class PreviewGameRepository : GameRepository {
    private val games = linkedMapOf<GameId, Game>()
    var saves = 0
        private set

    override suspend fun list(): List<Game> = games.values.toList()

    override suspend fun get(id: GameId): Game? = games[id]

    override suspend fun save(game: Game) {
        saves += 1
        games[game.id] = game
    }
}
