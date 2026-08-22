package com.micrantha.eyespie.sharing

import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.imaging.canonicalImageEmbedding

internal fun validatePortableGame(game: PortableGame) {
    require(isCanonicalId(game.gameId)) { "invalid portable game id" }
    require(isCanonicalText(game.gameName, MAX_GAME_NAME_CHARS)) { "invalid portable game name" }
    require(PLAYER_ID_REGEX.matches(game.creatorPlayerId)) { "invalid portable player id" }
    require(game.creatorPublicKey.size == P256_X963_PUBLIC_KEY_BYTES) { "invalid portable public key size" }
    require(game.creatorPublicKey.first() == P256_UNCOMPRESSED_PREFIX) { "invalid portable public key prefix" }
    require(game.things.size in 1..GAME_BUNDLE_MAX_THINGS) { "invalid portable thing count" }

    val seenIds = mutableSetOf<String>()
    game.things.forEach { thing ->
        require(isCanonicalId(thing.thingId)) { "invalid portable thing id" }
        require(seenIds.add(thing.thingId)) { "duplicate portable thing id" }
        require(isCanonicalText(thing.clueText, ClueAuthority.MAX_CLUE_LENGTH)) { "invalid portable clue" }
        canonicalImageEmbedding(thing.targetEmbedding)
        require(thing.matchThreshold.isFinite() && thing.matchThreshold in -1.0..1.0) {
            "invalid portable match threshold"
        }
    }
}

internal fun isCanonicalId(value: String): Boolean =
    value.isNotBlank() &&
        value.length <= MAX_ID_CHARS &&
        value == value.trim() &&
        value.none { it.isISOControl() }

internal fun isCanonicalText(value: String, maxChars: Int): Boolean =
    value.isNotBlank() &&
        value.length <= maxChars &&
        value == normalizeWhitespace(value) &&
        value.none { it == '\u0000' }

private fun normalizeWhitespace(value: String): String = value.trim().replace(Regex("\\s+"), " ")
