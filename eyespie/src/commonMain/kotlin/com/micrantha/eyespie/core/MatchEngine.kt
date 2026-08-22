package com.micrantha.eyespie.core

import kotlin.math.sqrt

class MatchEngine(
    private val threshold: Double = DEFAULT_THRESHOLD,
) {
    init {
        require(threshold in -1.0..1.0) { "threshold must be a cosine similarity" }
    }

    fun compare(target: List<Float>, guess: List<Float>): MatchResult {
        require(target.isNotEmpty()) { "target embedding must not be empty" }
        require(target.size == guess.size) { "embedding dimensions must match" }

        var dot = 0.0
        var targetMagnitude = 0.0
        var guessMagnitude = 0.0
        target.indices.forEach { index ->
            val targetValue = target[index].toDouble()
            val guessValue = guess[index].toDouble()
            dot += targetValue * guessValue
            targetMagnitude += targetValue * targetValue
            guessMagnitude += guessValue * guessValue
        }
        require(targetMagnitude > 0.0 && guessMagnitude > 0.0) {
            "embeddings must have non-zero magnitude"
        }

        val similarity = dot / (sqrt(targetMagnitude) * sqrt(guessMagnitude))
        return MatchResult(similarity = similarity, matched = similarity >= threshold)
    }

    companion object {
        const val DEFAULT_THRESHOLD: Double = 0.75
    }
}
