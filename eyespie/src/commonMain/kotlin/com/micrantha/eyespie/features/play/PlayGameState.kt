package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId

data class PlayGameContent(
    val gameName: String,
    val clueText: String,
    val matched: Boolean,
    val bestSimilarity: Double?,
    val foundCount: Int = if (matched) 1 else 0,
    val totalCount: Int = 1,
    val nextThingId: ThingId? = null,
) {
    init {
        require(totalCount >= 0) { "total count must not be negative" }
        require(foundCount in 0..totalCount) { "found count must be within total count" }
    }
}

sealed interface PlayFeedback {
    val similarity: Double
    val bestSimilarity: Double

    data class Matched(
        override val similarity: Double,
        override val bestSimilarity: Double,
        val foundCount: Int,
        val totalCount: Int,
        val nextThingId: ThingId?,
    ) : PlayFeedback {
        val completed: Boolean
            get() = foundCount >= totalCount || nextThingId == null
    }

    data class Mismatch(
        override val similarity: Double,
        override val bestSimilarity: Double,
    ) : PlayFeedback
}

data class PlayGameState(
    val gameId: GameId,
    val thingId: ThingId,
    val content: PlayGameContent? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val failure: PlayGameFailure? = null,
    val feedback: PlayFeedback? = null,
    val loadGeneration: Long = 0,
    val guessGeneration: Long = 0,
)
