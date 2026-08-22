package com.micrantha.eyespie.core

import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.clue.PlayableClue

data class Thing(
    val id: ThingId,
    val clueAuthority: ClueAuthority,
    val targetEmbedding: List<Float>,
    val matchThreshold: Double = MatchEngine.DEFAULT_THRESHOLD,
) {
    constructor(
        id: ThingId,
        clue: String,
        targetEmbedding: List<Float>,
        matchThreshold: Double = MatchEngine.DEFAULT_THRESHOLD,
    ) : this(
        id = id,
        clueAuthority = ClueAuthority.legacy(clue),
        targetEmbedding = targetEmbedding,
        matchThreshold = matchThreshold,
    )

    val clue: String
        get() = clueAuthority.clueText

    fun playableClue(): PlayableClue = clueAuthority.playable()

    init {
        require(targetEmbedding.isNotEmpty()) { "target embedding must not be empty" }
        require(matchThreshold in -1.0..1.0) { "match threshold must be a cosine similarity" }
    }
}
