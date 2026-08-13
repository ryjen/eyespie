package com.micrantha.eyespie.domain.entities

import okio.ByteString
import okio.ByteString.Companion.toByteString
import kotlin.math.sqrt

/**
 * Canonical semantic image-embedding contract used by Eyespie persistence and matching.
 *
 * The current release schema is a 1024-dimensional IEEE-754 float vector. In-memory binary
 * serialization is explicit big-endian float32 so Kotlin targets do not depend on native byte
 * order. Supabase/Postgres uses pgvector's textual `[f1,f2,...]` representation at the DTO
 * boundary; that representation must never be confused with the local binary blob.
 *
 * Model artifact identity is completed by #91's platform-runtime slice. This contract deliberately
 * fixes representation and dimensions first so Android, iOS, local persistence, and the backend
 * cannot silently choose different wire formats.
 */
object ImageEmbeddingContract {
    const val schemaVersion: Int = 1
    const val dimensions: Int = 1024
    const val bytesPerFloat: Int = 4
    const val encodedBytes: Int = dimensions * bytesPerFloat
    const val logicalModelId: String = "mobilenet-v3-small-100-224-embedder"
    const val androidModelAssetName: String = "mobilenet_v3_small_100_224_embedder.tflite"
}

typealias Embedding = ByteString

class InvalidEmbeddingException(message: String) : IllegalArgumentException(message)

fun Embedding.requireCanonical(): Embedding {
    if (size != ImageEmbeddingContract.encodedBytes) {
        throw InvalidEmbeddingException(
            "embedding byte count must be ${ImageEmbeddingContract.encodedBytes}, was $size"
        )
    }
    val values = floats()
    if (values.size != ImageEmbeddingContract.dimensions || values.any { !it.isFinite() }) {
        throw InvalidEmbeddingException("embedding must contain finite 1024-dimensional float values")
    }
    return this
}

fun Embedding.floats(): List<Float> {
    if (size % ImageEmbeddingContract.bytesPerFloat != 0) {
        throw InvalidEmbeddingException("embedding byte count must be divisible by 4")
    }

    val bytes = toByteArray()
    return List(bytes.size / ImageEmbeddingContract.bytesPerFloat) { i ->
        val offset = i * ImageEmbeddingContract.bytesPerFloat
        val bits = ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
        Float.fromBits(bits)
    }
}

fun List<Float>.toEmbedding(): Embedding {
    val bytes = ByteArray(size * ImageEmbeddingContract.bytesPerFloat)
    forEachIndexed { i, value ->
        val bits = value.toBits()
        val offset = i * ImageEmbeddingContract.bytesPerFloat
        bytes[offset] = (bits shr 24).toByte()
        bytes[offset + 1] = (bits shr 16).toByte()
        bytes[offset + 2] = (bits shr 8).toByte()
        bytes[offset + 3] = bits.toByte()
    }
    return bytes.toByteString()
}

fun List<Float>.toCanonicalEmbedding(): Embedding {
    if (size != ImageEmbeddingContract.dimensions) {
        throw InvalidEmbeddingException(
            "embedding dimension must be ${ImageEmbeddingContract.dimensions}, was $size"
        )
    }
    if (any { !it.isFinite() }) {
        throw InvalidEmbeddingException("embedding values must be finite")
    }
    return toEmbedding().requireCanonical()
}

fun Embedding.toPostgresVector(): String =
    requireCanonical().floats().joinToString(prefix = "[", postfix = "]", separator = ",")

fun String.toPostgresEmbedding(): Embedding {
    val value = trim()
    if (!value.startsWith("[") || !value.endsWith("]")) {
        throw InvalidEmbeddingException("Postgres embedding must use pgvector bracket syntax")
    }

    val content = value.substring(1, value.length - 1).trim()
    val values = if (content.isEmpty()) {
        emptyList()
    } else {
        content.split(',').mapIndexed { index, token ->
            token.trim().toFloatOrNull()
                ?: throw InvalidEmbeddingException("invalid float at embedding index $index")
        }
    }
    return values.toCanonicalEmbedding()
}

fun Embedding.cosineSimilarity(other: Embedding): Float {
    val a = requireCanonical().floats()
    val b = other.requireCanonical().floats()

    var dotProduct = 0.0
    var normA = 0.0
    var normB = 0.0

    for (i in a.indices) {
        dotProduct += a[i].toDouble() * b[i].toDouble()
        normA += a[i].toDouble() * a[i].toDouble()
        normB += b[i].toDouble() * b[i].toDouble()
    }

    val denominator = sqrt(normA) * sqrt(normB)
    return if (denominator <= 0.0) 0f else (dotProduct / denominator).toFloat()
}
