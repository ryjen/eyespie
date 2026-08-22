package com.micrantha.eyespie.sharing

enum class GameBundleFailureCode {
    TOO_LARGE,
    TRUNCATED,
    BAD_MAGIC,
    UNSUPPORTED_SCHEMA,
    UNSUPPORTED_CANONICALIZATION,
    UNSUPPORTED_SIGNATURE_ALGORITHM,
    UNSUPPORTED_EMBEDDING_CONTRACT,
    UNSUPPORTED_MATCH_POLICY,
    INVALID_LENGTH,
    INVALID_UTF8,
    INVALID_GAME_ID,
    INVALID_GAME_NAME,
    INVALID_PLAYER_ID,
    INVALID_PUBLIC_KEY,
    INVALID_THING_ID,
    DUPLICATE_THING_ID,
    INVALID_CLUE,
    INVALID_EMBEDDING,
    INVALID_MATCH_THRESHOLD,
    INVALID_SIGNATURE_LENGTH,
    TRAILING_BYTES,
}

sealed interface GameBundleDecodeResult {
    data class Success(val bundle: DecodedGameBundle) : GameBundleDecodeResult
    data class Failure(val code: GameBundleFailureCode) : GameBundleDecodeResult
}

internal class GameBundleFormatException(val code: GameBundleFailureCode) : Exception()

internal fun failGameBundle(code: GameBundleFailureCode): Nothing = throw GameBundleFormatException(code)
