package com.micrantha.eyespie.domain.entities

import com.micrantha.eyespie.features.players.domain.entities.Player
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class Thing(
    override val id: String,
    override val createdAt: Instant,
    val createdBy: Player.Ref,
    val guessed: Boolean,
    val guesses: List<Guess>,
    val imagePath: ImagePath,
    val location: Location.Point,
    val embedding: Embedding? = null
) : Entity, Creatable {

    data class Guess(
        val at: Instant,
        val by: Player.Ref,
        val correct: Boolean
    )

    /** Safe list/game projection. Authority-only image/location/proof/embedding are absent. */
    data class Listing(
        override val id: String,
        val nodeId: String,
        override val createdAt: Instant,
        val guessed: Boolean,
    ) : Entity, Creatable

    data class Match(
        override val id: String,
        val similarity: Float,
        val matched: Boolean,
    ) : Entity
}

typealias ThingList = List<Thing.Listing>

typealias ImagePath = String
