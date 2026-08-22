package com.micrantha.eyespie.sharing

import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_ID
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_CONTRACT_VERSION
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_DIMENSIONS
import com.micrantha.eyespie.imaging.canonicalImageEmbedding
import okio.Buffer

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
                failGameBundle(GameBundleFailureCode.UNSUPPORTED_EMBEDDING_CONTRACT)
            }
            val modelDigest = cursor.readBytes(EXPECTED_MODEL_SHA256.size)
            if (!modelDigest.contentEquals(EXPECTED_MODEL_SHA256)) {
                failGameBundle(GameBundleFailureCode.UNSUPPORTED_EMBEDDING_CONTRACT)
            }
            cursor.requireInt(
                IMAGE_EMBEDDING_DIMENSIONS,
                GameBundleFailureCode.UNSUPPORTED_EMBEDDING_CONTRACT,
            )

            val gameId = cursor.readString(MAX_ID_BYTES)
            if (!isCanonicalId(gameId)) failGameBundle(GameBundleFailureCode.INVALID_GAME_ID)

            val gameName = cursor.readString(MAX_GAME_NAME_BYTES)
            if (!isCanonicalText(gameName, MAX_GAME_NAME_CHARS)) {
                failGameBundle(GameBundleFailureCode.INVALID_GAME_NAME)
            }

            val creatorPlayerId = cursor.readString(MAX_PLAYER_ID_BYTES)
            if (!PLAYER_ID_REGEX.matches(creatorPlayerId)) {
                failGameBundle(GameBundleFailureCode.INVALID_PLAYER_ID)
            }

            val publicKey = cursor.readBytes(P256_X963_PUBLIC_KEY_BYTES)
            if (publicKey.firstOrNull() != P256_UNCOMPRESSED_PREFIX) {
                failGameBundle(GameBundleFailureCode.INVALID_PUBLIC_KEY)
            }

            val thingCount = cursor.readInt()
            if (thingCount !in 1..GAME_BUNDLE_MAX_THINGS) {
                failGameBundle(GameBundleFailureCode.INVALID_LENGTH)
            }

            val seenIds = mutableSetOf<String>()
            val things = ArrayList<PortableThing>(thingCount)
            repeat(thingCount) {
                val thingId = cursor.readString(MAX_ID_BYTES)
                if (!isCanonicalId(thingId)) failGameBundle(GameBundleFailureCode.INVALID_THING_ID)
                if (!seenIds.add(thingId)) failGameBundle(GameBundleFailureCode.DUPLICATE_THING_ID)

                val clueText = cursor.readString(MAX_CLUE_BYTES)
                if (!isCanonicalText(clueText, ClueAuthority.MAX_CLUE_LENGTH)) {
                    failGameBundle(GameBundleFailureCode.INVALID_CLUE)
                }

                cursor.requireInt(
                    GAME_BUNDLE_MATCH_POLICY_VERSION,
                    GameBundleFailureCode.UNSUPPORTED_MATCH_POLICY,
                )
                val matchThreshold = Double.fromBits(cursor.readLong())
                if (!matchThreshold.isFinite() || matchThreshold !in -1.0..1.0) {
                    failGameBundle(GameBundleFailureCode.INVALID_MATCH_THRESHOLD)
                }

                cursor.requireInt(
                    IMAGE_EMBEDDING_DIMENSIONS,
                    GameBundleFailureCode.INVALID_EMBEDDING,
                )
                val embedding = ArrayList<Float>(IMAGE_EMBEDDING_DIMENSIONS)
                repeat(IMAGE_EMBEDDING_DIMENSIONS) {
                    val value = Float.fromBits(cursor.readInt())
                    if (!value.isFinite()) failGameBundle(GameBundleFailureCode.INVALID_EMBEDDING)
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
                failGameBundle(GameBundleFailureCode.INVALID_SIGNATURE_LENGTH)
            }
            val signature = cursor.readBytes(signatureLength)
            if (!cursor.exhausted()) failGameBundle(GameBundleFailureCode.TRAILING_BYTES)

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
}
