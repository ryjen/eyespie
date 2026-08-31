package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.GameThumbnailCache
import com.micrantha.eyespie.game.LocalGameSnapshot

object GameDetailMapper {
    fun map(
        snapshot: LocalGameSnapshot,
        gameId: GameId,
        thumbnails: Map<ThingId, ByteArray> = emptyMap(),
    ): GameDetailContent? =
        snapshot.games.firstOrNull { it.id == gameId }?.let { game ->
            GameDetailContent(
                name = game.name,
                things = game.things.map { thing ->
                    GameDetailThing(
                        id = thing.id,
                        clueText = thing.clue.clueText,
                        matched = thing.progress?.matched ?: false,
                        bestSimilarity = thing.progress?.bestSimilarity,
                        thumbnail = thumbnails[thing.id],
                    )
                },
                localCreator = game.localCreator,
            )
        }
}
