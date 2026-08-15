package com.micrantha.eyespie.features.guess.ui

class ScanGuessMapper {

    operator fun invoke(state: ScanGuessState) = ScanGuessUiState(
        guessed = state.guessed,
        enabled = state.thingID != null,
        similarity = state.bestSimilarity
    )
}
