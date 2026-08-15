package com.micrantha.eyespie.sharing

/**
 * Platform document bridge for portable game bytes.
 *
 * Platform handles, URLs, paths, and picker objects stay behind this interface. Common application
 * code receives only bounded bytes and stable outcomes; bundle authority still comes exclusively
 * from [GameBundleService].
 */
interface GameDocumentTransfer {
    suspend fun read(): GameDocumentReadResult
    suspend fun write(suggestedFileName: String, bytes: ByteArray): GameDocumentWriteResult
}

sealed interface GameDocumentReadResult {
    data class Success(val bytes: ByteArray) : GameDocumentReadResult
    data object Cancelled : GameDocumentReadResult
    data object Busy : GameDocumentReadResult
    data object TooLarge : GameDocumentReadResult
    data object Failed : GameDocumentReadResult
}

sealed interface GameDocumentWriteResult {
    data object Success : GameDocumentWriteResult
    data object Cancelled : GameDocumentWriteResult
    data object Busy : GameDocumentWriteResult
    data object TooLarge : GameDocumentWriteResult
    data object Failed : GameDocumentWriteResult
}

fun suggestedGameBundleFileName(gameName: String, gameId: String): String {
    val base = gameName
        .lowercase()
        .map { character -> if (character.isLetterOrDigit()) character else '-' }
        .joinToString("")
        .replace(Regex("-+"), "-")
        .trim('-')
        .take(48)
        .ifBlank { "eyespie-game" }
    val suffix = gameId
        .filter { it.isLetterOrDigit() }
        .takeLast(12)
        .ifBlank { "local" }
    return "$base-$suffix.eyespie"
}
