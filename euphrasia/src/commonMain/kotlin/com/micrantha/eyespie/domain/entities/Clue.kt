package com.micrantha.eyespie.domain.entities

import okio.ByteString
import okio.Path

data class Clues(
    val labels: LabelProof? = null,
    val location: LocationProof? = null, // TODO: Geofence
    val colors: ColorProof? = null,
    val detections: DetectProof? = null
)

data class Proof(
    val clues: Clues?,
    val location: Location.Point?,
    val match: Embedding?,
    val image: Path,
    val name: String?,
    val playerID: String
)

typealias LabelProof = Set<LabelClue>

typealias ColorProof = Set<ColorClue>

typealias LocationProof = LocationClue

typealias DetectProof = Set<DetectClue>

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

data class DataClue(
    override val data: String,
    override val confidence: Float
) : EquatableClue<String>(), RankedClue<String>

typealias LabelClue = DataClue
typealias ColorClue = DataClue
typealias RhymeClue = DataClue
typealias DetectClue = DataClue

data class LocationClue(
    override val data: Location.Data, // TODO: make a geofence area
) : SortedClue<Location.Data>

typealias Embedding = ByteString
