package com.micrantha.bluebell.domain.security

import okio.ByteString.Companion.encodeUtf8
import okio.Source
import okio.blackholeSink
import okio.buffer
import okio.use
import okio.HashingSink.Companion.sha256 as okioSha256

fun sha256(input: String): String {
    return input.encodeUtf8().sha256().hex()
}

fun sha256(source: Source): String {
    val hashingSink = okioSha256(blackholeSink())
    source.use { src ->
        hashingSink.buffer().use { sink ->
            sink.writeAll(src)
        }
    }
    return hashingSink.hash.hex()
}
