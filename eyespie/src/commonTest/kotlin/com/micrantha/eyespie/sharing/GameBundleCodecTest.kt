package com.micrantha.eyespie.sharing

import com.micrantha.eyespie.identity.playerIdFor
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_SHA256
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_DIMENSIONS
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameBundleCodecTest {
    private val codec = GameBundleCodec()

    @Test
    fun canonicalEncodingIsDeterministicAndRoundTripsExactly() {
        val game = portableGame()
        val unsignedA = codec.encodeUnsigned(game)
        val unsignedB = codec.encodeUnsigned(game)
        val signature = byteArrayOf(0x30, 0x06, 0x02, 0x01, 0x01, 0x02, 0x01, 0x01)

        assertContentEquals(unsignedA, unsignedB)

        val signed = codec.encodeSigned(unsignedA, signature)
        val decoded = assertIs<GameBundleDecodeResult.Success>(codec.decode(signed)).bundle

        assertTrue(game.equivalentTo(decoded.game))
        assertContentEquals(unsignedA, decoded.unsignedBytes)
        assertContentEquals(signature, decoded.signature)
    }

    @Test
    fun wireUsesExplicitBigEndianFloatAndDoubleBitPatterns() {
        val embedding = MutableList(IMAGE_EMBEDDING_DIMENSIONS) { 0f }
        embedding[0] = 1.0f
        embedding[1] = -2.0f
        val unsigned = codec.encodeUnsigned(portableGame(embedding = embedding, threshold = 0.5))

        val embeddingStart = unsigned.size - (IMAGE_EMBEDDING_DIMENSIONS * 4)
        val embeddingCountOffset = embeddingStart - 4
        val thresholdOffset = embeddingCountOffset - 8
        val policyOffset = thresholdOffset - 4

        assertContentEquals(intBytes(GAME_BUNDLE_MATCH_POLICY_VERSION), unsigned.slice(policyOffset, 4))
        assertContentEquals(longBytes(0.5.toRawBits()), unsigned.slice(thresholdOffset, 8))
        assertContentEquals(intBytes(IMAGE_EMBEDDING_DIMENSIONS), unsigned.slice(embeddingCountOffset, 4))
        assertContentEquals(intBytes(1.0f.toRawBits()), unsigned.slice(embeddingStart, 4))
        assertContentEquals(intBytes((-2.0f).toRawBits()), unsigned.slice(embeddingStart + 4, 4))
    }

    @Test
    fun parserRejectsOversizeTruncationAndTrailingBytes() {
        assertFailure(
            GameBundleFailureCode.TOO_LARGE,
            ByteArray(GAME_BUNDLE_MAX_BYTES + 1),
        )

        val unsigned = codec.encodeUnsigned(portableGame())
        val signed = codec.encodeSigned(unsigned, byteArrayOf(1, 2, 3, 4))
        listOf(0, 1, 7, 8, 12, unsigned.size - 1, unsigned.size, signed.size - 1).forEach { cut ->
            assertTrue(codec.decode(signed.copyOfRange(0, cut)) is GameBundleDecodeResult.Failure)
        }

        assertFailure(
            GameBundleFailureCode.TRAILING_BYTES,
            signed + byteArrayOf(0x01),
        )
    }

    @Test
    fun parserRejectsUnsupportedVersionsAndEmbeddingIdentity() {
        val unsigned = codec.encodeUnsigned(portableGame())
        val signature = byteArrayOf(1, 2, 3)

        assertFailure(
            GameBundleFailureCode.UNSUPPORTED_SCHEMA,
            codec.encodeSigned(unsigned.withInt(8, 2), signature),
        )

        val digest = IMAGE_EMBEDDER_MODEL_SHA256.hexBytes()
        val digestOffset = unsigned.indexOfSubsequence(digest)
        assertTrue(digestOffset >= 0)
        val wrongDigest = unsigned.copyOf().also { it[digestOffset] = (it[digestOffset].toInt() xor 0x01).toByte() }
        assertFailure(
            GameBundleFailureCode.UNSUPPORTED_EMBEDDING_CONTRACT,
            codec.encodeSigned(wrongDigest, signature),
        )
    }

    @Test
    fun parserRejectsMalformedUtf8DuplicateIdsAndNonFiniteNumbers() {
        val signature = byteArrayOf(1, 2, 3)

        val utf8Unsigned = codec.encodeUnsigned(portableGame())
        val nameOffset = utf8Unsigned.indexOfSubsequence("Road Trip".encodeToByteArray())
        assertTrue(nameOffset >= 0)
        val invalidUtf8 = utf8Unsigned.copyOf().also {
            it[nameOffset] = 0xc3.toByte()
            it[nameOffset + 1] = 0x28
        }
        assertFailure(
            GameBundleFailureCode.INVALID_UTF8,
            codec.encodeSigned(invalidUtf8, signature),
        )

        val twoThings = portableGame(
            things = listOf(
                portableThing("thing:one"),
                portableThing("thing:two"),
            ),
        )
        val duplicateUnsigned = codec.encodeUnsigned(twoThings)
        val secondIdOffset = duplicateUnsigned.lastIndexOfSubsequence("thing:two".encodeToByteArray())
        assertTrue(secondIdOffset >= 0)
        "thing:one".encodeToByteArray().copyInto(duplicateUnsigned, secondIdOffset)
        assertFailure(
            GameBundleFailureCode.DUPLICATE_THING_ID,
            codec.encodeSigned(duplicateUnsigned, signature),
        )

        val numericUnsigned = codec.encodeUnsigned(portableGame())
        val embeddingStart = numericUnsigned.size - IMAGE_EMBEDDING_DIMENSIONS * 4
        val nonFiniteEmbedding = numericUnsigned.withInt(embeddingStart, Float.NaN.toRawBits())
        assertFailure(
            GameBundleFailureCode.INVALID_EMBEDDING,
            codec.encodeSigned(nonFiniteEmbedding, signature),
        )

        val thresholdOffset = embeddingStart - 4 - 8
        val nonFiniteThreshold = numericUnsigned.withLong(thresholdOffset, Double.NaN.toRawBits())
        assertFailure(
            GameBundleFailureCode.INVALID_MATCH_THRESHOLD,
            codec.encodeSigned(nonFiniteThreshold, signature),
        )
    }

    @Test
    fun encoderRejectsNonCanonicalOrUnsafePortableValues() {
        assertFailsWith<IllegalArgumentException> {
            codec.encodeUnsigned(portableGame().copy(gameName = "  padded  "))
        }
        assertFailsWith<IllegalArgumentException> {
            codec.encodeUnsigned(
                portableGame(
                    things = listOf(
                        portableThing("thing:same"),
                        portableThing("thing:same"),
                    ),
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            codec.encodeUnsigned(portableGame(embedding = List(IMAGE_EMBEDDING_DIMENSIONS) { Float.NaN }))
        }
    }

    private fun assertFailure(expected: GameBundleFailureCode, bytes: ByteArray) {
        val failure = assertIs<GameBundleDecodeResult.Failure>(codec.decode(bytes))
        assertEquals(expected, failure.code)
    }
}

private fun portableGame(
    embedding: List<Float> = unitEmbedding(),
    threshold: Double = 0.75,
    things: List<PortableThing>? = null,
): PortableGame {
    val publicKey = testPublicKey(7)
    return PortableGame(
        gameId = "game:test",
        gameName = "Road Trip",
        creatorPlayerId = playerIdFor(publicKey).value,
        creatorPublicKey = publicKey,
        things = things ?: listOf(
            PortableThing(
                thingId = "thing:test",
                clueText = "Something striped",
                targetEmbedding = embedding,
                matchThreshold = threshold,
            ),
        ),
    )
}

private fun portableThing(id: String): PortableThing = PortableThing(
    thingId = id,
    clueText = "Find the target",
    targetEmbedding = unitEmbedding(),
    matchThreshold = 0.75,
)

private fun unitEmbedding(index: Int = 0): List<Float> =
    List(IMAGE_EMBEDDING_DIMENSIONS) { if (it == index) 1f else 0f }

internal fun testPublicKey(seed: Int): ByteArray = ByteArray(65) { index ->
    when (index) {
        0 -> 0x04
        else -> ((seed + index * 17) and 0xff).toByte()
    }
}

private fun ByteArray.slice(offset: Int, length: Int): ByteArray = copyOfRange(offset, offset + length)

private fun ByteArray.withInt(offset: Int, value: Int): ByteArray = copyOf().also {
    intBytes(value).copyInto(it, offset)
}

private fun ByteArray.withLong(offset: Int, value: Long): ByteArray = copyOf().also {
    longBytes(value).copyInto(it, offset)
}

private fun intBytes(value: Int): ByteArray = byteArrayOf(
    (value ushr 24).toByte(),
    (value ushr 16).toByte(),
    (value ushr 8).toByte(),
    value.toByte(),
)

private fun longBytes(value: Long): ByteArray = ByteArray(8) { index ->
    (value ushr ((7 - index) * 8)).toByte()
}

private fun ByteArray.indexOfSubsequence(needle: ByteArray): Int {
    if (needle.isEmpty()) return 0
    for (index in 0..size - needle.size) {
        if (needle.indices.all { this[index + it] == needle[it] }) return index
    }
    return -1
}

private fun ByteArray.lastIndexOfSubsequence(needle: ByteArray): Int {
    if (needle.isEmpty()) return size
    for (index in size - needle.size downTo 0) {
        if (needle.indices.all { this[index + it] == needle[it] }) return index
    }
    return -1
}

private fun String.hexBytes(): ByteArray = ByteArray(length / 2) { index ->
    substring(index * 2, index * 2 + 2).toInt(16).toByte()
}
