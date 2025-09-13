package com.micrantha.bluebell.domain.security

import io.ktor.utils.io.core.toByteArray
import okio.Buffer
import okio.HashingSink.Companion.md5
import okio.HashingSink.Companion.sha256
import okio.Source
import okio.blackholeSink
import okio.buffer
import okio.use

fun sha256(input: String): String = Buffer().use {
    it.write(input.toByteArray()).flush()
    sha256(it).hash.hex()
}

fun sha256(source: Source): String {
    val hashingSink = sha256(blackholeSink())
    val bufferedSink = hashingSink.buffer()
    source.use { src ->
        bufferedSink.use { sink ->
            sink.writeAll(src)
            Unit
        }
    }
    return hashingSink.hash.hex()
}

fun md5(input: String): String = Buffer().use {
    it.write(input.toByteArray()).flush()
    md5(it).hash.hex()
}
