package com.micrantha.eyespie.sharing

import com.micrantha.eyespie.clue.ClueAuthoringResult
import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.clue.ClueOrigin
import com.micrantha.eyespie.core.Game
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.GameRepository
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.PlayerIdentityRepository
import com.micrantha.eyespie.core.Thing
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.identity.SigningIdentity
import com.micrantha.eyespie.identity.playerIdFor
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_DIMENSIONS
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class GameBundleServiceTest {
    @Test
    fun localCreatorExportsSignedPlayableProjectionWithoutExpectedAnswer() = runTest {
        val key = testPublicKey(11)
        val identity = PlayerIdentity(playerIdFor(key), "Agent")
        val repository = InMemoryBundleGameRepository(localGame(identity.id))
        val signing = DeterministicSigningIdentity(key)
        val service = GameBundleService(FixedBundleIdentity(identity), signing, repository)

        val exported = assertIs<GameBundleExportResult.Success>(service.export(GameId("game:test")))
        val decoded = assertIs<GameBundleDecodeResult.Success>(GameBundleCodec().decode(exported.bytes)).bundle

        assertEquals(identity.id.value, decoded.game.creatorPlayerId)
        assertEquals("Something striped", decoded.game.things.single().clueText)
        assertFalse(exported.bytes.containsSubsequence("secret-crosswalk-answer".encodeToByteArray()))
        assertTrue(signing.verify(key, decoded.unsignedBytes, decoded.signature))
    }

    @Test
    fun nonCreatorCannotResignImportedOrForeignGame() = runTest {
        val localKey = testPublicKey(13)
        val localIdentity = PlayerIdentity(playerIdFor(localKey), "Agent")
        val foreignCreator = playerIdFor(testPublicKey(17))
        val repository = InMemoryBundleGameRepository(localGame(foreignCreator))
        val service = GameBundleService(
            FixedBundleIdentity(localIdentity),
            DeterministicSigningIdentity(localKey),
            repository,
        )

        assertEquals(
            GameBundleExportFailureCode.NOT_LOCAL_CREATOR,
            assertIs<GameBundleExportResult.Failure>(service.export(GameId("game:test"))).code,
        )
    }

    @Test
    fun importVerifiesSignatureBeforePersistingAndStoresSharedAuthority() = runTest {
        val sourceKey = testPublicKey(19)
        val sourceIdentity = PlayerIdentity(playerIdFor(sourceKey), "Creator")
        val sourceRepository = InMemoryBundleGameRepository(localGame(sourceIdentity.id))
        val sourceService = GameBundleService(
            FixedBundleIdentity(sourceIdentity),
            DeterministicSigningIdentity(sourceKey),
            sourceRepository,
        )
        val bytes = assertIs<GameBundleExportResult.Success>(
            sourceService.export(GameId("game:test")),
        ).bytes

        val destinationKey = testPublicKey(23)
        val destinationIdentity = PlayerIdentity(playerIdFor(destinationKey), "Player")
        val destinationRepository = InMemoryBundleGameRepository()
        val destinationService = GameBundleService(
            FixedBundleIdentity(destinationIdentity),
            DeterministicSigningIdentity(destinationKey),
            destinationRepository,
        )

        assertEquals(
            GameBundleImportResult.Imported(GameId("game:test")),
            destinationService.import(bytes),
        )
        val imported = destinationRepository.get(GameId("game:test"))!!
        assertEquals(sourceIdentity.id, imported.creator)
        val authority = imported.things.single().clueAuthority
        assertEquals(ClueOrigin.SHARED, authority.origin)
        assertEquals("Something striped", authority.clueText)
        assertNull(authority.expectedAnswer)
        assertNull(authority.generatedProvenance)
    }

    @Test
    fun repeatedImportIsIdempotentEvenWhenCreatorLocalAuthorityIsRicher() = runTest {
        val key = testPublicKey(29)
        val identity = PlayerIdentity(playerIdFor(key), "Creator")
        val repository = InMemoryBundleGameRepository(localGame(identity.id))
        val service = GameBundleService(
            FixedBundleIdentity(identity),
            DeterministicSigningIdentity(key),
            repository,
        )
        val bytes = assertIs<GameBundleExportResult.Success>(service.export(GameId("game:test"))).bytes

        assertEquals(
            GameBundleImportResult.AlreadyPresent(GameId("game:test")),
            service.import(bytes),
        )
        assertEquals(0, repository.importSaveCalls)
        assertEquals("secret-crosswalk-answer", repository.get(GameId("game:test"))!!.things.single().clueAuthority.expectedAnswer)
    }

    @Test
    fun conflictingSameGameIdDoesNotOverwriteLocalAuthority() = runTest {
        val sourceKey = testPublicKey(31)
        val sourceId = playerIdFor(sourceKey)
        val sourceService = GameBundleService(
            FixedBundleIdentity(PlayerIdentity(sourceId, "Creator")),
            DeterministicSigningIdentity(sourceKey),
            InMemoryBundleGameRepository(localGame(sourceId)),
        )
        val bytes = assertIs<GameBundleExportResult.Success>(sourceService.export(GameId("game:test"))).bytes

        val destinationRepository = InMemoryBundleGameRepository(
            localGame(sourceId).copy(name = "Different local game"),
        )
        val destinationKey = testPublicKey(37)
        val destinationService = GameBundleService(
            FixedBundleIdentity(PlayerIdentity(playerIdFor(destinationKey), "Player")),
            DeterministicSigningIdentity(destinationKey),
            destinationRepository,
        )

        assertEquals(
            GameBundleImportResult.Conflict(GameId("game:test")),
            destinationService.import(bytes),
        )
        assertEquals("Different local game", destinationRepository.get(GameId("game:test"))!!.name)
        assertEquals(0, destinationRepository.importSaveCalls)
    }

    @Test
    fun changedPayloadOrSignatureFailsBeforePersistence() = runTest {
        val sourceKey = testPublicKey(41)
        val sourceId = playerIdFor(sourceKey)
        val sourceService = GameBundleService(
            FixedBundleIdentity(PlayerIdentity(sourceId, "Creator")),
            DeterministicSigningIdentity(sourceKey),
            InMemoryBundleGameRepository(localGame(sourceId)),
        )
        val original = assertIs<GameBundleExportResult.Success>(sourceService.export(GameId("game:test"))).bytes

        val destinationKey = testPublicKey(43)
        val destinationRepository = InMemoryBundleGameRepository()
        val destinationService = GameBundleService(
            FixedBundleIdentity(PlayerIdentity(playerIdFor(destinationKey), "Player")),
            DeterministicSigningIdentity(destinationKey),
            destinationRepository,
        )

        val clueOffset = original.indexOfSubsequence("Something striped".encodeToByteArray())
        assertTrue(clueOffset >= 0)
        val changedPayload = original.copyOf().also { it[clueOffset] = 'X'.code.toByte() }
        assertEquals(
            GameBundleImportFailureCode.INVALID_SIGNATURE,
            assertIs<GameBundleImportResult.Failure>(destinationService.import(changedPayload)).code,
        )

        val changedSignature = original.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 0x01).toByte() }
        assertEquals(
            GameBundleImportFailureCode.INVALID_SIGNATURE,
            assertIs<GameBundleImportResult.Failure>(destinationService.import(changedSignature)).code,
        )
        assertTrue(destinationRepository.values.isEmpty())
        assertEquals(0, destinationRepository.importSaveCalls)
    }

    @Test
    fun creatorPlayerIdMismatchFailsBeforeSignatureOrPersistence() = runTest {
        val sourceKey = testPublicKey(47)
        val sourceId = playerIdFor(sourceKey)
        val sourceService = GameBundleService(
            FixedBundleIdentity(PlayerIdentity(sourceId, "Creator")),
            DeterministicSigningIdentity(sourceKey),
            InMemoryBundleGameRepository(localGame(sourceId)),
        )
        val original = assertIs<GameBundleExportResult.Success>(sourceService.export(GameId("game:test"))).bytes
        val playerIdBytes = sourceId.value.encodeToByteArray()
        val idOffset = original.indexOfSubsequence(playerIdBytes)
        assertTrue(idOffset >= 0)
        val mismatched = original.copyOf().also {
            val lastHexOffset = idOffset + playerIdBytes.lastIndex
            it[lastHexOffset] = if (it[lastHexOffset] == '0'.code.toByte()) '1'.code.toByte() else '0'.code.toByte()
        }

        val destinationKey = testPublicKey(53)
        val repository = InMemoryBundleGameRepository()
        val service = GameBundleService(
            FixedBundleIdentity(PlayerIdentity(playerIdFor(destinationKey), "Player")),
            DeterministicSigningIdentity(destinationKey),
            repository,
        )

        assertEquals(
            GameBundleImportFailureCode.CREATOR_ID_MISMATCH,
            assertIs<GameBundleImportResult.Failure>(service.import(mismatched)).code,
        )
        assertTrue(repository.values.isEmpty())
    }
}

private class FixedBundleIdentity(
    private val identity: PlayerIdentity,
) : PlayerIdentityRepository {
    override suspend fun current(): PlayerIdentity = identity
}

private class DeterministicSigningIdentity(
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

private class InMemoryBundleGameRepository(initial: Game? = null) : GameRepository {
    val values = linkedMapOf<GameId, Game>()
    var importSaveCalls = 0
        private set

    init {
        if (initial != null) values[initial.id] = initial
    }

    override suspend fun list(): List<Game> = values.values.toList()

    override suspend fun get(id: GameId): Game? = values[id]

    override suspend fun save(game: Game) {
        importSaveCalls += 1
        values[game.id] = game
    }
}

private fun localGame(creator: PlayerId): Game = Game(
    id = GameId("game:test"),
    name = "Road Trip",
    creator = creator,
    things = listOf(
        Thing(
            id = ThingId("thing:test"),
            clueAuthority = accepted(
                ClueAuthority.manual(
                    clueText = "Something striped",
                    expectedAnswer = "secret-crosswalk-answer",
                ),
            ),
            targetEmbedding = List(IMAGE_EMBEDDING_DIMENSIONS) { if (it == 0) 1f else 0f },
            matchThreshold = 0.75,
        ),
    ),
)

private fun accepted(result: ClueAuthoringResult): ClueAuthority = when (result) {
    is ClueAuthoringResult.Accepted -> result.authority
    is ClueAuthoringResult.Rejected -> error("expected accepted clue authority")
}

private fun ByteArray.containsSubsequence(needle: ByteArray): Boolean = indexOfSubsequence(needle) >= 0

private fun ByteArray.indexOfSubsequence(needle: ByteArray): Int {
    if (needle.isEmpty()) return 0
    if (needle.size > size) return -1
    for (index in 0..size - needle.size) {
        if (needle.indices.all { this[index + it] == needle[it] }) return index
    }
    return -1
}
