package com.micrantha.eyespie.sharing

/**
 * Presents an already-authorized portable game artifact to the platform-native share surface.
 *
 * Presentation is not delivery authority: [GameSharePresentationResult.Presented] means the OS
 * accepted the handoff and displayed/started its chooser, not that any receiving application
 * delivered or persisted the file.
 */
interface GameSharePresenter {
    suspend fun present(suggestedFileName: String, bytes: ByteArray): GameSharePresentationResult
}

sealed interface GameSharePresentationResult {
    data object Presented : GameSharePresentationResult
    data object Busy : GameSharePresentationResult
    data object TooLarge : GameSharePresentationResult
    data object Failed : GameSharePresentationResult
}
