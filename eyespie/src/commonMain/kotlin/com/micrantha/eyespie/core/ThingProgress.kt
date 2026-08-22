package com.micrantha.eyespie.core

data class ThingProgress(
    val gameId: GameId,
    val thingId: ThingId,
    val playerId: PlayerId,
    val matched: Boolean,
    val bestSimilarity: Double? = null,
) {
    init {
        require(bestSimilarity == null || bestSimilarity in -1.0..1.0) {
            "best similarity must be a cosine similarity"
        }
    }
}
