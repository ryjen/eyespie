package com.micrantha.eyespie.identity

import com.micrantha.eyespie.core.PlayerId
import okio.ByteString.Companion.toByteString

fun playerIdFor(publicKey: ByteArray): PlayerId {
    require(publicKey.size == P256_X963_PUBLIC_KEY_SIZE) {
        "P-256 public key must be a 65-byte X9.63 uncompressed point"
    }
    require(publicKey.first() == P256_X963_UNCOMPRESSED_PREFIX) {
        "P-256 public key must use X9.63 uncompressed-point encoding"
    }

    val digest = publicKey.toByteString().sha256().hex()
    return PlayerId("p256:$digest")
}

internal const val P256_X963_PUBLIC_KEY_SIZE = 65
internal const val P256_X963_UNCOMPRESSED_PREFIX: Byte = 0x04
