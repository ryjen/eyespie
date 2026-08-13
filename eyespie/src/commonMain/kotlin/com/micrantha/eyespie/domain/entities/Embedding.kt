package com.micrantha.eyespie.domain.entities

import kotlin.math.sqrt

const val ALPHA_EMBEDDING_DIMENSIONS = 1024

enum class EmbeddingNormalization(val wireValue: String) {
    ModelDefined("model_defined"),
}

enum class EmbeddingSimilarity(val wireValue: String) {
    Cosine("cosine"),
}

/**
 * Immutable identity for an embedding space.
 *
 * [modelId] must identify one logical model revision. Once artifact provenance is pinned, the
 * model identifier must not be reused for a different artifact. The persistence form also carries
 * dimensions, normalization, and similarity so same-sized but semantically incompatible vectors
 * cannot be compared accidentally.
 */
data class EmbeddingMetadata(
    val modelId: String,
    val dimensions: Int,
    val normalization: EmbeddingNormalization,
    val similarity: EmbeddingSimilarity,
) {
    init {
        require(modelId.isNotBlank()) { "embedding model id must not be blank" }
        require('|' !in modelId) { "embedding model id contains a reserved separator" }
        require(dimensions > 0) { "embedding dimensions must be positive" }
    }

    val persistenceId: String
        get() = listOf(
            modelId,
            dimensions.toString(),
            normalization.wireValue,
            similarity.wireValue,
        ).joinToString("|")

    companion object {
        fun parse(persistenceId: String): EmbeddingMetadata {
            val parts = persistenceId.split('|')
            require(parts.size == 4) { "invalid embedding metadata" }

            val normalization = EmbeddingNormalization.entries.singleOrNull {
                it.wireValue == parts[2]
            } ?: throw IllegalArgumentException("unsupported embedding normalization")
            val similarity = EmbeddingSimilarity.entries.singleOrNull {
                it.wireValue == parts[3]
            } ?: throw IllegalArgumentException("unsupported embedding similarity")

            return EmbeddingMetadata(
                modelId = parts[0],
                dimensions = parts[1].toIntOrNull()
                    ?: throw IllegalArgumentException("invalid embedding dimensions"),
                normalization = normalization,
                similarity = similarity,
            )
        }
    }
}

/**
 * Validated embedding vector with explicit schema/model identity.
 *
 * Values are copied on construction and must be finite and exactly match [metadata.dimensions].
 * Binary persistence uses deterministic big-endian IEEE-754 float32. Supabase/pgvector persistence
 * uses the decimal vector representation from [toPgVector].
 */
class Embedding private constructor(
    val metadata: EmbeddingMetadata,
    values: List<Float>,
) {
    private val components = values.toList()

    val values: List<Float>
        get() = components

    init {
        require(components.size == metadata.dimensions) {
            "embedding dimension mismatch: expected ${metadata.dimensions}, got ${components.size}"
        }
        require(components.all(Float::isFinite)) { "embedding contains non-finite values" }
    }

    fun requireCompatible(other: Embedding) {
        require(metadata.persistenceId == other.metadata.persistenceId) {
            "incompatible embedding metadata"
        }
    }

    fun cosineSimilarity(other: Embedding): Float {
        requireCompatible(other)

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        for (index in components.indices) {
            val a = components[index].toDouble()
            val b = other.components[index].toDouble()
            dotProduct += a * b
            normA += a * a
            normB += b * b
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator <= 0.0) 0f else (dotProduct / denominator).toFloat()
    }

    fun toByteArray(): ByteArray = ByteArray(components.size * FLOAT_BYTES).also { bytes ->
        components.forEachIndexed { index, value ->
            val bits = value.toBits()
            val offset = index * FLOAT_BYTES
            bytes[offset] = (bits ushr 24).toByte()
            bytes[offset + 1] = (bits ushr 16).toByte()
            bytes[offset + 2] = (bits ushr 8).toByte()
            bytes[offset + 3] = bits.toByte()
        }
    }

    fun toPgVector(): String = components.joinToString(
        prefix = "[",
        postfix = "]",
        separator = ",",
    )

    override fun equals(other: Any?): Boolean =
        other is Embedding && metadata == other.metadata && components == other.components

    override fun hashCode(): Int = 31 * metadata.hashCode() + components.hashCode()

    override fun toString(): String =
        "Embedding(model=${metadata.modelId}, dimensions=${metadata.dimensions})"

    companion object {
        private const val FLOAT_BYTES = 4

        fun of(metadata: EmbeddingMetadata, values: List<Float>): Embedding =
            Embedding(metadata, values)

        fun fromByteArray(metadata: EmbeddingMetadata, bytes: ByteArray): Embedding {
            require(bytes.size == metadata.dimensions * FLOAT_BYTES) {
                "embedding byte length does not match metadata dimensions"
            }

            val values = List(metadata.dimensions) { index ->
                val offset = index * FLOAT_BYTES
                val bits = ((bytes[offset].toInt() and 0xFF) shl 24) or
                    ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                    ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                    (bytes[offset + 3].toInt() and 0xFF)
                Float.fromBits(bits)
            }
            return Embedding(metadata, values)
        }

        fun fromPgVector(metadata: EmbeddingMetadata, encoded: String): Embedding {
            val trimmed = encoded.trim()
            require(trimmed.startsWith('[') && trimmed.endsWith(']')) {
                "invalid pgvector embedding"
            }
            val content = trimmed.substring(1, trimmed.length - 1)
            val values = if (content.isBlank()) {
                emptyList()
            } else {
                content.split(',').map { component ->
                    component.trim().toFloatOrNull()
                        ?: throw IllegalArgumentException("invalid pgvector component")
                }
            }
            return Embedding(metadata, values)
        }
    }
}
