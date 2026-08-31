package com.micrantha.eyespie.core

import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.clue.PlayableClue

data class Thing(
    val id: ThingId,
    val clueAuthority: ClueAuthority,
    val targetEmbedding: List<Float>,
    val matchThreshold: Double = MatchEngine.DEFAULT_THRESHOLD,
    /**
     * Optional local-only display thumbnail (downscaled capture bytes). It is a
     * device cache for UX only: matching authority stays the embedding, and this
     * is never serialized into a portable .eyespie bundle. Null for imported games
     * or when the cache is unavailable.
     */
    val targetThumbnail: ByteArray? = null,
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Thing) return false
        return id == other.id &&
            clueAuthority == other.clueAuthority &&
            targetEmbedding == other.targetEmbedding &&
            matchThreshold == other.matchThreshold &&
            (targetThumbnail == null && other.targetThumbnail == null ||
                targetThumbnail != null && other.targetThumbnail != null &&
                targetThumbnail.contentEquals(other.targetThumbnail))
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + clueAuthority.hashCode()
        result = 31 * result + targetEmbedding.hashCode()
        result = 31 * result + matchThreshold.hashCode()
        result = 31 * result + (targetThumbnail?.contentHashCode() ?: 0)
        return result
    }
}
