package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.LocalGameSnapshot

object PlayGameMapper {
    fun map(snapshot: LocalGameSnapshot, gameId: GameId, thingId: ThingId): PlayGameContent? {
        val game = snapshot.games.firstOrNull { it.id == gameId } ?: return null
        val currentIndex = game.things.indexOfFirst { it.id == thingId }
        if (currentIndex < 0) return null
        val thing = game.things[currentIndex]
        val nextUnmatched = game.things.firstOrNull { candidate ->
            candidate.id != thingId && candidate.progress?.matched != true
        }
        return PlayGameContent(
            gameName = game.name,
            clueText = thing.clue.clueText,
            matched = thing.progress?.matched ?: false,
            bestSimilarity = thing.progress?.bestSimilarity,
            clueNumber = currentIndex + 1,
            clueCount = game.things.size,
            matchedClueCount = game.things.count { it.progress?.matched == true },
            nextThingId = nextUnmatched?.id,
        )
    }
}
