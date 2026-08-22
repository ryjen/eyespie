package com.micrantha.eyespie.sharing

/** Portable allowlisted game state. Creator-only clue authority is intentionally absent. */
data class PortableGame(
    val gameId: String,
    val gameName: String,
    val creatorPlayerId: String,
    val creatorPublicKey: ByteArray,
    val things: List<PortableThing>,
) {
    fun equivalentTo(other: PortableGame): Boolean =
        gameId == other.gameId &&
            gameName == other.gameName &&
            creatorPlayerId == other.creatorPlayerId &&
            creatorPublicKey.contentEquals(other.creatorPublicKey) &&
            things.size == other.things.size &&
            things.indices.all { things[it].equivalentTo(other.things[it]) }
}

data class PortableThing(
    val thingId: String,
    val clueText: String,
    val targetEmbedding: List<Float>,
    val matchThreshold: Double,
) {
    fun equivalentTo(other: PortableThing): Boolean =
        thingId == other.thingId &&
            clueText == other.clueText &&
            matchThreshold.toRawBits() == other.matchThreshold.toRawBits() &&
            targetEmbedding.size == other.targetEmbedding.size &&
            targetEmbedding.indices.all {
                targetEmbedding[it].toRawBits() == other.targetEmbedding[it].toRawBits()
            }
}
