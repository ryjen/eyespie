package com.micrantha.eyespie.domain.entities

import kotlinx.serialization.Serializable
import okio.ByteString

data class Proof(
    val clues: AiProof?,
    val location: Location?,
    val embedding: Embedding = ByteString.EMPTY,
)

typealias Clues = Set<AiClue>

typealias AiProof = Set<AiClue>

typealias GuessProof = Set<GuessClue>

typealias LocationProof = LocationClue

sealed interface Clue<T> {
    val data: T
    fun display() = data.toString()
}

sealed interface SortedClue<T : Comparable<T>> : Clue<T>, Comparable<SortedClue<T>> {
    override fun compareTo(other: SortedClue<T>) = data.compareTo(other.data)
}

sealed interface RankedClue<T : Comparable<T>> : Clue<T>, Comparable<RankedClue<T>> {
    val confidence: Float
    override fun compareTo(other: RankedClue<T>) = confidence.compareTo(other.confidence)
}

abstract class EquatableClue<T> : Clue<T> {

    override fun hashCode() = data.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other is EquatableClue<T>) {
            return data == other.data
        }
        return super.equals(other)
    }
}

@Serializable
data class AiClue(
    override val data: String,
    override val confidence: Float,
    val answer: String,
) : EquatableClue<String>(), RankedClue<String>

@Serializable
data class GuessClue(
    override val data: String
) : EquatableClue<String>()

data class LocationClue(
    override val data: Location.Data, // TODO: make a geofence area
) : SortedClue<Location.Data>

typealias Embedding = ByteString

fun Embedding.floats(): List<Float> {
    val bytes = toByteArray()
    return List(bytes.size / 4) { i ->
        val bits = (bytes[i * 4].toInt() and 0xFF shl 24) or
                (bytes[i * 4 + 1].toInt() and 0xFF shl 16) or
                (bytes[i * 4 + 2].toInt() and 0xFF shl 8) or
                (bytes[i * 4 + 3].toInt() and 0xFF)
        Float.fromBits(bits)
    }
}

fun List<Float>.toEmbedding(): Embedding {
    val bytes = ByteArray(size * 4)
    forEachIndexed { i, f ->
        val bits = f.toBits()
        bytes[i * 4] = (bits shr 24).toByte()
        bytes[i * 4 + 1] = (bits shr 16).toByte()
        bytes[i * 4 + 2] = (bits shr 8).toByte()
        bytes[i * 4 + 3] = bits.toByte()
    }
    return ByteString.of(*bytes)
}

fun Embedding.cosineSimilarity(other: Embedding): Float {
    val a = floats()
    val b = other.floats()
    if (a.size != b.size || a.isEmpty()) return 0f
    
    var dotProduct = 0.0
    var normA = 0.0
    var normB = 0.0
    
    for (i in a.indices) {
        dotProduct += a[i].toDouble() * b[i].toDouble()
        normA += a[i].toDouble() * a[i].toDouble()
        normB += b[i].toDouble() * b[i].toDouble()
    }
    
    val denom = kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)
    return if (denom <= 0.0) 0f else (dotProduct / denom).toFloat()
}
