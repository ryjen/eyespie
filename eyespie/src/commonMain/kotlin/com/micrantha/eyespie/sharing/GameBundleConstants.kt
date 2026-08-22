package com.micrantha.eyespie.sharing

import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_SHA256

const val GAME_BUNDLE_SCHEMA_VERSION = 1
const val GAME_BUNDLE_CANONICALIZATION_VERSION = 1
const val GAME_BUNDLE_SIGNATURE_ALGORITHM_P256_ECDSA_SHA256_DER = 1
const val GAME_BUNDLE_MATCH_POLICY_VERSION = 1
const val GAME_BUNDLE_MAX_BYTES = 4 * 1024 * 1024
const val GAME_BUNDLE_MAX_THINGS = 256
const val GAME_BUNDLE_MAX_SIGNATURE_BYTES = 128

internal val GAME_BUNDLE_MAGIC = "EYESPIE1".encodeToByteArray()
internal val EXPECTED_MODEL_SHA256 = decodeHex(IMAGE_EMBEDDER_MODEL_SHA256)

internal const val MAX_ID_CHARS = 160
internal const val MAX_ID_BYTES = 640
internal const val MAX_GAME_NAME_CHARS = 80
internal const val MAX_GAME_NAME_BYTES = 320
internal const val MAX_CLUE_BYTES = ClueAuthority.MAX_CLUE_LENGTH * 4
internal const val MAX_PLAYER_ID_BYTES = 69
internal const val MAX_MODEL_ID_BYTES = 160
internal const val P256_X963_PUBLIC_KEY_BYTES = 65
internal const val P256_UNCOMPRESSED_PREFIX: Byte = 0x04
internal val PLAYER_ID_REGEX = Regex("p256:[0-9a-f]{64}")

private fun decodeHex(value: String): ByteArray {
    require(value.length % 2 == 0) { "hex input must have an even length" }
    return ByteArray(value.length / 2) { index ->
        val offset = index * 2
        value.substring(offset, offset + 2).toInt(16).toByte()
    }
}
