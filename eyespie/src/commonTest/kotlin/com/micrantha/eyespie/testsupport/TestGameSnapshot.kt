package com.micrantha.eyespie.testsupport

import com.micrantha.eyespie.clue.PlayableClue
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.ThingProgress
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.game.LocalGameSummary
import com.micrantha.eyespie.game.PlayableThingSummary

fun testGameSnapshot(
    displayName: String = "Agent",
    playerId: PlayerId = PlayerId("p256:${"0".repeat(64)}"),
    games: List<LocalGameSummary> = emptyList(),
): LocalGameSnapshot = LocalGameSnapshot(
    identity = PlayerIdentity(playerId, displayName),
    games = games,
)

fun testGameSummary(
    name: String = "Trip",
    things: List<PlayableThingSummary> = emptyList(),
    localCreator: Boolean = false,
): LocalGameSummary = LocalGameSummary(
    id = testGameId,
    name = name,
    things = things,
    localCreator = localCreator,
)

fun testPlayableThingSummary(
    clueText: String = "Find it",
    matched: Boolean = false,
    bestSimilarity: Double? = null,
): PlayableThingSummary = PlayableThingSummary(
    id = testThingId,
    clue = PlayableClue(clueText),
    progress = if (matched || bestSimilarity != null) {
        ThingProgress(
            gameId = testGameId,
            thingId = testThingId,
            playerId = PlayerId("p256:${"0".repeat(64)}"),
            matched = matched,
            bestSimilarity = bestSimilarity,
        )
    } else {
        null
    },
)
