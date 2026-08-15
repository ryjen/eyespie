package com.micrantha.eyespie.sharing

const val EYESPIE_FILE_EXTENSION = "eyespie"
const val EYESPIE_MIME_TYPE = "application/vnd.eyespie.game"

sealed interface GameBundleDocumentWriteResult {
    data object Success : GameBundleDocumentWriteResult
    data object Cancelled : GameBundleDocumentWriteResult
    data object TooLarge : GameBundleDocumentWriteResult
    data object Failed : GameBundleDocumentWriteResult
}

sealed interface GameBundleDocumentReadResult {
    data class Success(val bytes: ByteArray) : GameBundleDocumentReadResult
    data object Cancelled : GameBundleDocumentReadResult
    data object TooLarge : GameBundleDocumentReadResult
    data object Failed : GameBundleDocumentReadResult
}

/**
 * Platform document boundary for the already-canonical signed bundle bytes.
 *
 * Implementations own Android Uri / iOS URL lifecycle and never expose those objects or paths to
 * common game state. User cancellation is a normal result, not an exception.
 */
interface GameBundleDocumentGateway {
    suspend fun export(
        suggestedFileName: String,
        bytes: ByteArray,
    ): GameBundleDocumentWriteResult

    suspend fun import(): GameBundleDocumentReadResult
}

fun suggestedGameBundleFileName(gameName: String, gameId: String): String {
    val base = gameName
        .trim()
        .lowercase()
        .map { character ->
            when {
                character.isLetterOrDigit() -> character
                character == '-' || character == '_' -> character
                else -> '-'
            }
        }
        .joinToString(separator = "")
        .replace(Regex("-+"), "-")
        .trim('-')
        .take(48)
        .ifBlank { "eyespie-game" }
    val suffix = gameId
        .filter(Char::isLetterOrDigit)
        .takeLast(12)
        .ifBlank { "game" }
    return "$base-$suffix.$EYESPIE_FILE_EXTENSION"
}
