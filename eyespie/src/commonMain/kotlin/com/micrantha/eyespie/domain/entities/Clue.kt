package com.micrantha.eyespie.domain.entities

import kotlinx.serialization.Serializable
import okio.ByteString
import okio.Path

data class Proof(
    val clues: AiProof?,
    val location: Location?,
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
