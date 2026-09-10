package com.micrantha.eyespie.app

import com.micrantha.eyespie.core.GameId
import kotlinx.coroutines.flow.Flow

sealed interface ExternalAppIntent {
    data class OpenLocalGame(val gameId: GameId) : ExternalAppIntent
}

interface ExternalAppIntentSource {
    val intents: Flow<ExternalAppIntent>
}

/**
 * Parse the normalized components of an app-owned deep link.
 *
 * URL parsing/percent-decoding remains platform-owned. This function accepts exactly one decoded
 * path segment and deliberately supports only local navigation; it never accepts portable game
 * bytes or other authority-bearing metadata.
 */
fun parseEyespieDeepLink(
    scheme: String?,
    host: String?,
    pathSegments: List<String>,
): ExternalAppIntent? {
    if (!scheme.equals("eyespie", ignoreCase = true)) return null
    if (!host.equals("game", ignoreCase = true)) return null
    if (pathSegments.size != 1) return null

    val gameId = pathSegments.single()
    if (gameId.isEmpty() || gameId.length > MAX_DEEP_LINK_GAME_ID_LENGTH) return null
    if (!gameId.all(::isSafeGameIdCharacter)) return null

    return ExternalAppIntent.OpenLocalGame(GameId(gameId))
}

private fun isSafeGameIdCharacter(character: Char): Boolean =
    character.isLetterOrDigit() || character in "-._~:"

private const val MAX_DEEP_LINK_GAME_ID_LENGTH = 128
