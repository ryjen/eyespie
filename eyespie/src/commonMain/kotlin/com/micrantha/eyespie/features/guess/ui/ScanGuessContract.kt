package com.micrantha.eyespie.features.guess.ui

import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlinx.serialization.Serializable

data class ScanGuessState(
    val thing: Thing? = null,
    val bestSimilarity: Float? = null
)

data class ScanGuessUiState(
    val guessed: Boolean,
    val enabled: Boolean,
    val similarity: Float? = null
)

@Serializable
data class ScanGuessArgs(
    val id: String
)

sealed interface ScanGuessAction {
    data class ImageCaptured(val image: CameraImage) : ScanGuessAction
    data object ThingMatched : ScanGuessAction
    data object ThingNotFound : ScanGuessAction
    data class SimilarityUpdated(val similarity: Float?) : ScanGuessAction

    data object Load : ScanGuessAction
    data class Loaded(val thing: Thing) : ScanGuessAction
}
