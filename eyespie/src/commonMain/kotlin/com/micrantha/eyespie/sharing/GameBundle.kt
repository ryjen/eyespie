package com.micrantha.eyespie.sharing

import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_ID
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_SHA256
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_CONTRACT_VERSION
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_DIMENSIONS
import com.micrantha.eyespie.imaging.canonicalImageEmbedding
import okio.Buffer

const val GAME_BUNDLE_SCHEMA_VERSION = 1
const val GAME_BUNDLE_CANONICALIZATION_VERSION = 1
const val GAME_BUNDLE_SIGNATURE_ALGORITHM_P256_ECDSA_SHA256_DER = 1
const val GAME_BUNDLE_MATCH_POLICY_VERSION = 1
const val GAME_BUNDLE_MAX_BYTES = 4 * 1024 * 1024
const val GAME_BUNDLE_MAX_THINGS = 256
const val GAME_BUNDLE_MAX_SIGNATURE_BYTES = 128

private val GAME_BUNDLE_MAGIC = "EYESPIE1".encodeToByteArray()
private val EXPECTED_MODEL_SHA256 = decodeHex(IMAGE_EMBEDDER_MODEL_SHA256)

/** Portable allowlisted game state. Creator-only clue authority is intentionally absent. */
data class PortableGame(
    val gameId: String,
    val gameName: String,
    val creatorPlayerId: String,
    val creatorPublicKey: ByteArray,
    val things: List<PortableThing>,
) {
    fun equivalentTo(other: PortableGame): Boolean =
        gameId == other.gameId &&
            gameName == other.gameName &&
            creatorPlayerId == other.creatorPlayerId &&
            creatorPublicKey.contentEquals(other.creatorPublicKey) &&
            things.size == other.things.size &&
            things.indices.all { things[it].equivalentTo(other.things[it]) }
}

data class PortableThing(
    val thingId: String,
    val clueText: String,
    val targetEmbedding: List<Float>,
    val matchThreshold: Double,
) {
    fun equivalentTo(other: PortableThing): Boolean =
        thingId == other.thingId &&
            clueText == other.clueText &&
            matchThreshold.toRawBits() == other.matchThreshold.toRawBits() &&
            targetEmbedding.size == other.targetEmbedding.size &&
            targetEmbedding.indices.all {
                targetEmbedding[it].toRawBits() == other.targetEmbedding[it].toRawBits()
            }
}

data class DecodedGameBundle(
    val game: PortableGame,
    val unsignedBytes: ByteArray,
    val signature: ByteArray,
)

enum class GameBundleFailureCode {
    TOO_LARGE,
    TRUNCATED,
    BAD_MAGIC,
    UNSUPPORTED_SCHEMA,
    UNSUPPORTED_CANONICALIZATION,
    UNSUPPORTED_SIGNATURE_ALGORITHM,
    UNSUPPORTED_EMBEDDING_CONTRACT,
    UNSUPPORTED_MATCH_POLICY,
    INVALID_LENGTH,
    INVALID_UTF8,
    INVALID_GAME_ID,
    INVALID_GAME_NAME,
    INVALID_PLAYER_ID,
    INVALID_PUBLIC_KEY,
    INVALID_THING_ID,
    DUPLICATE_THING_ID,
    INVALID_CLUE,
    INVALID_EMBEDDING,
    INVALID_MATCH_THRESHOLD,
    INVALID_SIGNATURE_LENGTH,
    TRAILING_BYTES,
}

sealed interface GameBundleDecodeResult {
    data class Success(val bundle: DecodedGameBundle) : GameBundleDecodeResult
    data class Failure(val code: GameBundleFailureCode) : GameBundleDecodeResult
}

class GameBundleCodec {
    fun encodeUnsigned(game: PortableGame): ByteArray {
        validatePortableGame(game)

        val buffer = Buffer()
        buffer.write(GAME_BUNDLE_MAGIC)
        buffer.writeInt(GAME_BUNDLE_SCHEMA_VERSION)
        buffer.writeInt(GAME_BUNDLE_CANONICALIZATION_VERSION)
        buffer.writeInt(GAME_BUNDLE_SIGNATURE_ALGORITHM_P256_ECDSA_SHA256_DER)
        buffer.writeInt(IMAGE_EMBEDDING_CONTRACT_VERSION)
        buffer.writeLengthPrefixedUtf8(IMAGE_EMBEDDER_MODEL_ID)
        buffer.write(EXPECTED_MODEL_SHA256)
        buffer.writeInt(IMAGE_EMBEDDING_DIMENSIONS)

        buffer.writeLengthPrefixedUtf8(game.gameId)
        buffer.writeLengthPrefixedUtf8(game.gameName)
        buffer.writeLengthPrefixedUtf8(game.creatorPlayerId)
        buffer.write(game.creatorPublicKey)
        buffer.writeInt(game.things.size)

        game.things.forEach { thing ->
            buffer.writeLengthPrefixedUtf8(thing.thingId)
            buffer.writeLengthPrefixedUtf8(thing.clueText)
            buffer.writeInt(GAME_BUNDLE_MATCH_POLICY_VERSION)
            buffer.writeLong(thing.matchThreshold.toRawBits())
            buffer.writeInt(thing.targetEmbedding.size)
            thing.targetEmbedding.forEach { value ->
                buffer.writeInt(value.toRawBits())
            }
        }

        check(buffer.size <= GAME_BUNDLE_MAX_BYTES.toLong()) { "canonical bundle exceeds maximum size" }
        return buffer.readByteArray()
    }

    fun encodeSigned(unsignedBytes: ByteArray, signature: ByteArray): ByteArray {
        require(unsignedBytes.isNotEmpty()) { "unsigned bundle bytes must not be empty" }
        require(signature.isNotEmpty() && signature.size <= GAME_BUNDLE_MAX_SIGNATURE_BYTES) {
            "signature length is outside the supported bound"
        }
        require(
            unsignedBytes.size.toLong() + 4L + signature.size.toLong() <= GAME_BUNDLE_MAX_BYTES.toLong(),
        ) { "signed bundle exceeds maximum size" }

        return Buffer().apply {
            write(unsignedBytes)
            writeInt(signature.size)
            write(signature)
        }.readByteArray()
    }

    fun decode(bytes: ByteArray): GameBundleDecodeResult {
        if (bytes.size > GAME_BUNDLE_MAX_BYTES) {
            return GameBundleDecodeResult.Failure(GameBundleFailureCode.TOO_LARGE)
        }
        if (bytes.isEmpty()) {
            return GameBundleDecodeResult.Failure(GameBundleFailureCode.TRUNCATED)
        }

        return try {
            val cursor = ByteCursor(bytes)
            cursor.expectMagic()

            cursor.requireInt(GAME_BUNDLE_SCHEMA_VERSION, GameBundleFailureCode.UNSUPPORTED_SCHEMA)
            cursor.requireInt(
                GAME_BUNDLE_CANONICALIZATION_VERSION,
                GameBundleFailureCode.UNSUPPORTED_CANONICALIZATION,
            )
            cursor.requireInt(
                GAME_BUNDLE_SIGNATURE_ALGORITHM_P256_ECDSA_SHA256_DER,
                GameBundleFailureCode.UNSUPPORTED_SIGNATURE_ALGORITHM,
            )
            cursor.requireInt(
                IMAGE_EMBEDDING_CONTRACT_VERSION,
                GameBundleFailureCode.UNSUPPORTED_EMBEDDING_CONTRACT,
            )

            val modelId = cursor.readString(MAX_MODEL_ID_BYTES)
            if (modelId != IMAGE_EMBEDDER_MODEL_ID) {
                fail(GameBundleFailureCode.UNSUPPORTED_EMBEDDING_CONTRACT)
            }
            val modelDigest = cursor.readBytes(EXPECTED_MODEL_SHA256.size)
            if (!modelDigest.contentEquals(EXPECTED_MODEL_SHA256)) {
                fail(GameBundleFailureCode.UNSUPPORTED_EMBEDDING_CONTRACT)
            }
            cursor.requireInt(
                IMAGE_EMBEDDING_DIMENSIONS,
                GameBundleFailureCode.UNSUPPORTED_EMBEDDING_CONTRACT,
            )

            val gameId = cursor.readString(MAX_ID_BYTES)
            if (!isCanonicalId(gameId)) fail(GameBundleFailureCode.INVALID_GAME_ID)

            val gameName = cursor.readString(MAX_GAME_NAME_BYTES)
            if (!isCanonicalText(gameName, MAX_GAME_NAME_CHARS)) {
                fail(GameBundleFailureCode.INVALID_GAME_NAME)
            }

            val creatorPlayerId = cursor.readString(MAX_PLAYER_ID_BYTES)
            if (!PLAYER_ID_REGEX.matches(creatorPlayerId)) {
                fail(GameBundleFailureCode.INVALID_PLAYER_ID)
            }

            val publicKey = cursor.readBytes(P256_X963_PUBLIC_KEY_BYTES)
            if (publicKey.firstOrNull() != P256_UNCOMPRESSED_PREFIX) {
                fail(GameBundleFailureCode.INVALID_PUBLIC_KEY)
            }

            val thingCount = cursor.readInt()
            if (thingCount !in 1..GAME_BUNDLE_MAX_THINGS) {
                fail(GameBundleFailureCode.INVALID_LENGTH)
            }

            val seenIds = mutableSetOf<String>()
            val things = ArrayList<PortableThing>(thingCount)
            repeat(thingCount) {
                val thingId = cursor.readString(MAX_ID_BYTES)
                if (!isCanonicalId(thingId)) fail(GameBundleFailureCode.INVALID_THING_ID)
                if (!seenIds.add(thingId)) fail(GameBundleFailureCode.DUPLICATE_THING_ID)

                val clueText = cursor.readString(MAX_CLUE_BYTES)
                if (!isCanonicalText(clueText, ClueAuthority.MAX_CLUE_LENGTH)) {
                    fail(GameBundleFailureCode.INVALID_CLUE)
                }

                cursor.requireInt(
                    GAME_BUNDLE_MATCH_POLICY_VERSION,
                    GameBundleFailureCode.UNSUPPORTED_MATCH_POLICY,
                )
                val matchThreshold = Double.fromBits(cursor.readLong())
                if (!matchThreshold.isFinite() || matchThreshold !in -1.0..1.0) {
                    fail(GameBundleFailureCode.INVALID_MATCH_THRESHOLD)
                }

                cursor.requireInt(
                    IMAGE_EMBEDDING_DIMENSIONS,
                    GameBundleFailureCode.INVALID_EMBEDDING,
                )
                val embedding = ArrayList<Float>(IMAGE_EMBEDDING_DIMENSIONS)
                repeat(IMAGE_EMBEDDING_DIMENSIONS) {
                    val value = Float.fromBits(cursor.readInt())
                    if (!value.isFinite()) fail(GameBundleFailureCode.INVALID_EMBEDDING)
                    embedding += value
                }

                things += PortableThing(
                    thingId = thingId,
                    clueText = clueText,
                    targetEmbedding = canonicalImageEmbedding(embedding),
                    matchThreshold = matchThreshold,
                )
            }

            val unsignedLength = cursor.position
            val signatureLength = cursor.readInt()
            if (signatureLength !in 1..GAME_BUNDLE_MAX_SIGNATURE_BYTES) {
                fail(GameBundleFailureCode.INVALID_SIGNATURE_LENGTH)
            }
            val signature = cursor.readBytes(signatureLength)
            if (!cursor.exhausted()) fail(GameBundleFailureCode.TRAILING_BYTES)

            GameBundleDecodeResult.Success(
                DecodedGameBundle(
                    game = PortableGame(
                        gameId = gameId,
                        gameName = gameName,
                        creatorPlayerId = creatorPlayerId,
                        creatorPublicKey = publicKey,
                        things = things,
                    ),
                    unsignedBytes = bytes.copyOfRange(0, unsignedLength),
                    signature = signature,
                ),
            )
        } catch (failure: GameBundleFormatException) {
            GameBundleDecodeResult.Failure(failure.code)
        }
    }

    private fun validatePortableGame(game: PortableGame) {
        require(isCanonicalId(game.gameId)) { "invalid portable game id" }
        require(isCanonicalText(game.gameName, MAX_GAME_NAME_CHARS)) { "invalid portable game name" }
        require(PLAYER_ID_REGEX.matches(game.creatorPlayerId)) { "invalid portable player id" }
        require(game.creatorPublicKey.size == P256_X963_PUBLIC_KEY_BYTES) { "invalid portable public key size" }
        require(game.creatorPublicKey.first() == P256_UNCOMPRESSED_PREFIX) { "invalid portable public key prefix" }
        require(game.things.size in 1..GAME_BUNDLE_MAX_THINGS) { "invalid portable thing count" }

        val seenIds = mutableSetOf<String>()
        game.things.forEach { thing ->
            require(isCanonicalId(thing.thingId)) { "invalid portable thing id" }
            require(seenIds.add(thing.thingId)) { "duplicate portable thing id" }
            require(isCanonicalText(thing.clueText, ClueAuthority.MAX_CLUE_LENGTH)) { "invalid portable clue" }
            canonicalImageEmbedding(thing.targetEmbedding)
            require(thing.matchThreshold.isFinite() && thing.matchThreshold in -1.0..1.0) {
                "invalid portable match threshold"
            }
        }
    }
}

private fun Buffer.writeLengthPrefixedUtf8(value: String) {
    val encoded = value.encodeToByteArray()
    writeInt(encoded.size)
    write(encoded)
}

private class ByteCursor(private val bytes: ByteArray) {
    var position: Int = 0
        private set

    fun exhausted(): Boolean = position == bytes.size

    fun expectMagic() {
        val actual = readBytes(GAME_BUNDLE_MAGIC.size)
        if (!actual.contentEquals(GAME_BUNDLE_MAGIC)) fail(GameBundleFailureCode.BAD_MAGIC)
    }

    fun requireInt(expected: Int, code: GameBundleFailureCode) {
        if (readInt() != expected) fail(code)
    }

    fun readString(maxBytes: Int): String {
        val length = readInt()
        if (length < 0 || length > maxBytes) fail(GameBundleFailureCode.INVALID_LENGTH)
        val encoded = readBytes(length)
        return try {
            encoded.decodeToString(throwOnInvalidSequence = true)
        } catch (_: Exception) {
            fail(GameBundleFailureCode.INVALID_UTF8)
        }
    }

    fun readInt(): Int {
        requireRemaining(4)
        val result =
            (unsigned(bytes[position]) shl 24) or
                (unsigned(bytes[position + 1]) shl 16) or
                (unsigned(bytes[position + 2]) shl 8) or
                unsigned(bytes[position + 3])
        position += 4
        return result
    }

    fun readLong(): Long {
        requireRemaining(8)
        var result = 0L
        repeat(8) {
            result = (result shl 8) or unsigned(bytes[position + it]).toLong()
        }
        position += 8
        return result
    }

    fun readBytes(length: Int): ByteArray {
        if (length < 0) fail(GameBundleFailureCode.INVALID_LENGTH)
        requireRemaining(length)
        val result = bytes.copyOfRange(position, position + length)
        position += length
        return result
    }

    private fun requireRemaining(length: Int) {
        if (length > bytes.size - position) fail(GameBundleFailureCode.TRUNCATED)
    }

    private fun unsigned(value: Byte): Int = value.toInt() and 0xff
}

private class GameBundleFormatException(val code: GameBundleFailureCode) : Exception()

private fun fail(code: GameBundleFailureCode): Nothing = throw GameBundleFormatException(code)

private fun isCanonicalId(value: String): Boolean =
    value.isNotBlank() &&
        value.length <= MAX_ID_CHARS &&
        value == value.trim() &&
        value.none { it.isISOControl() }

private fun isCanonicalText(value: String, maxChars: Int): Boolean =
    value.isNotBlank() &&
        value.length <= maxChars &&
        value == normalizeWhitespace(value) &&
        value.none { it == '\u0000' }

private fun normalizeWhitespace(value: String): String = value.trim().replace(Regex("\\s+"), " ")

private fun decodeHex(value: String): ByteArray {
    require(value.length % 2 == 0) { "hex input must have an even length" }
    return ByteArray(value.length / 2) { index ->
        val offset = index * 2
        value.substring(offset, offset + 2).toInt(16).toByte()
    }
}

private const val MAX_ID_CHARS = 160
private const val MAX_ID_BYTES = 640
private const val MAX_GAME_NAME_CHARS = 80
private const val MAX_GAME_NAME_BYTES = 320
private const val MAX_CLUE_BYTES = ClueAuthority.MAX_CLUE_LENGTH * 4
private const val MAX_PLAYER_ID_BYTES = 69
private const val MAX_MODEL_ID_BYTES = 160
private const val P256_X963_PUBLIC_KEY_BYTES = 65
private const val P256_UNCOMPRESSED_PREFIX: Byte = 0x04
private val PLAYER_ID_REGEX = Regex("p256:[0-9a-f]{64}")
