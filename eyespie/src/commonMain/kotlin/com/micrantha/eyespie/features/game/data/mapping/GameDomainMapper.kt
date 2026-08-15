package com.micrantha.eyespie.features.game.data.mapping

import com.micrantha.eyespie.domain.entities.Game
import com.micrantha.eyespie.domain.entities.Game.Limits
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.features.game.data.model.GameData
import com.micrantha.eyespie.features.game.data.model.GameRemoteDetails
import com.micrantha.eyespie.features.game.data.model.SafeGamePlayerData
import com.micrantha.eyespie.features.game.data.model.SafeGameThingData
import com.micrantha.eyespie.features.players.domain.entities.Player
import com.micrantha.eyespie.graphql.GameListQuery
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class GameDomainMapper {

    fun data(node: GameListQuery.Node) = GameData(
        id = node.id,
        title = node.name,
        createdAt = node.created_at,
        expiresAt = node.expires,
        creatorId = "", // Not available in query
        playerCount = node.players?.totalCount ?: 0
    )

    fun list(data: GameData) = Game.Listing(
        id = data.id,
        nodeId = data.id,
        name = data.title,
        createdAt = Instant.parse(data.createdAt),
        expiresAt = data.expiresAt?.let { Instant.parse(it) } ?: Instant.DISTANT_FUTURE,
        totalPlayers = data.playerCount,
        totalThings = 0
    )

    fun list(data: GameListQuery.Node) = Game.Listing(
        id = data.id,
        nodeId = data.nodeId,
        name = data.name,
        createdAt = Instant.parse(data.created_at),
        expiresAt = Instant.parse(data.expires),
        totalPlayers = data.players?.totalCount ?: 0,
        totalThings = data.things?.totalCount ?: 0
    )

    fun map(data: GameData) = Game(
        id = data.id,
        name = data.title,
        createdAt = Instant.parse(data.createdAt),
        expires = data.expiresAt?.let { Instant.parse(it) } ?: Instant.DISTANT_FUTURE,
        turnDuration = Duration.ZERO,
        players = emptyList(),
        things = emptyList(),
        limits = Limits(0..0, 0..0)
    )

    fun map(details: GameRemoteDetails) = Game(
        id = details.id,
        name = details.name,
        createdAt = Instant.parse(details.createdAt),
        expires = Instant.parse(details.expiresAt),
        turnDuration = Duration.parse(details.turnDuration),
        players = details.players.map(::player),
        things = details.things.map(::thing),
        limits = Limits(
            player = IntRange(details.minPlayers ?: 1, details.maxPlayers ?: 10),
            thing = IntRange(details.minThings ?: 1, details.maxThings ?: 10)
        )
    )

    private fun player(data: SafeGamePlayerData) = Player.Listing(
        id = data.id,
        nodeId = data.nodeId,
        createdAt = Instant.parse(data.createdAt),
        name = "${data.firstName} ${data.lastName}",
        score = data.score,
    )

    private fun thing(data: SafeGameThingData) = Thing.Listing(
        id = data.id,
        nodeId = data.id,
        createdAt = Instant.parse(data.createdAt),
        guessed = data.guessed,
    )
}
