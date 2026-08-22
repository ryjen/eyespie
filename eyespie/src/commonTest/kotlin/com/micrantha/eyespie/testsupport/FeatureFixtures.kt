package com.micrantha.eyespie.testsupport

import com.micrantha.eyespie.clue.ClueAuthoringResult
import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.MatchResult
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.core.ThingProgress
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.imaging.CapturedImage

val testGameId = GameId("game-1")
val testThingId = ThingId("thing-1")
val testPlayerId = PlayerId("player-1")

fun testImage(): CapturedImage = CapturedImage.fromEncoded(byteArrayOf(1, 2, 3))

fun testGuessOutcome(
    similarity: Double = 0.9,
    matched: Boolean = true,
): GuessOutcome {
    val clue = when (val authored = ClueAuthority.manual("Find it", "it")) {
        is ClueAuthoringResult.Accepted -> authored.authority.playable()
        is ClueAuthoringResult.Rejected -> error("fixture clue rejected")
    }
    return GuessOutcome(
        gameId = testGameId,
        thingId = testThingId,
        clue = clue,
        match = MatchResult(similarity = similarity, matched = matched),
        progress = ThingProgress(
            gameId = testGameId,
            thingId = testThingId,
            playerId = testPlayerId,
            matched = matched,
            bestSimilarity = similarity,
        ),
    )
}
