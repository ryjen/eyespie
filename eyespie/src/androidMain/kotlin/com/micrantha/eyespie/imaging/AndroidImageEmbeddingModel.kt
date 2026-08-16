package com.micrantha.eyespie.imaging

import android.content.Context
import java.nio.ByteBuffer
import java.security.MessageDigest

internal data class AndroidImageEmbeddingModel(
    val sha256: String,
    private val bytes: ByteArray,
) {
    fun directBuffer(): ByteBuffer = ByteBuffer.allocateDirect(bytes.size).apply {
        put(bytes)
        rewind()
    }
}

internal fun loadAndroidImageEmbeddingModel(context: Context): AndroidImageEmbeddingModel {
    val bytes = context.assets.open(IMAGE_EMBEDDER_MODEL_FILE).use { it.readBytes() }
    check(bytes.isNotEmpty()) { "image embedder model asset is empty" }

    val digest = MessageDigest.getInstance("SHA-256").digest(bytes).toLowerHex()
    check(digest == IMAGE_EMBEDDER_MODEL_SHA256) {
        "image embedder model asset failed integrity verification"
    }

    return AndroidImageEmbeddingModel(
        sha256 = digest,
        bytes = bytes,
    )
}

private fun ByteArray.toLowerHex(): String = buildString(size * 2) {
    for (byte in this@toLowerHex) {
        val value = byte.toInt() and 0xff
        append(HEX[value ushr 4])
        append(HEX[value and 0x0f])
    }
}

private const val HEX = "0123456789abcdef"
