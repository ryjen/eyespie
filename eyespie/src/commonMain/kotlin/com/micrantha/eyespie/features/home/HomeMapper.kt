package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.LocalGameSnapshot

object HomeMapper {
    fun map(
        snapshot: LocalGameSnapshot,
        thumbnails: Map<String, Map<ThingId, ByteArray>> = emptyMap(),
    ): HomeContent = HomeContent(
        identityDisplayName = snapshot.identity.displayName,
        identityIdSuffix = snapshot.identity.id.value.takeLast(12),
        games = snapshot.games.map { game ->
            HomeGame(
                id = game.id,
                name = game.name,
                things = game.things.map { thing ->
                    HomeThing(
                        id = thing.id,
                        clueText = thing.clue.clueText,
                        matched = thing.progress?.matched ?: false,
                        bestSimilarity = thing.progress?.bestSimilarity,
                    )
                },
                localCreator = game.localCreator,
            )
        },
        thumbnails = thumbnails,
    )
}
