package com.micrantha.eyespie.core

import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.clue.PlayableClue
import kotlin.jvm.JvmInline
import kotlin.math.sqrt

@JvmInline
value class PlayerId(val value: String)

@JvmInline
value class GameId(val value: String)

@JvmInline
value class ThingId(val value: String)

data class PlayerIdentity(
    val id: PlayerId,
    val displayName: String,
)

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

data class Game(
    val id: GameId,
    val name: String,
    val creator: PlayerId,
    val things: List<Thing> = emptyList(),
)

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

data class MatchResult(
    val similarity: Double,
    val matched: Boolean,
)

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

interface PlayerIdentityRepository {
    suspend fun current(): PlayerIdentity
}

interface GameRepository {
    suspend fun list(): List<Game>
    suspend fun get(id: GameId): Game?
    suspend fun save(game: Game)
}

interface ThingProgressRepository {
    suspend fun get(gameId: GameId, thingId: ThingId, playerId: PlayerId): ThingProgress?
    suspend fun list(gameId: GameId, playerId: PlayerId): List<ThingProgress>
    suspend fun save(progress: ThingProgress)
}

interface GameTransport {
    val available: Boolean
}

interface CloudSyncAdapter {
    val enabled: Boolean
}
