package com.micrantha.eyespie.features.scan.entities

import androidx.compose.ui.graphics.painter.Painter

data class ScanEditUiState(
    val clues: Collection<ScanClue>,
    val image: Painter?,
    val authoringMode: ClueAuthoringMode,
    val manualClue: String,
    val manualAnswer: String,
    val generationUnavailable: Boolean,
    val canUseGenerated: Boolean,
    val isBusy: Boolean,
    val enabled: Boolean,
<<<<<<< Updated upstream
    val isError: Boolean,
||||||| Stash base
    val isError: Boolean
=======
    val isError: Boolean,
    val errorMessage: String? = null
>>>>>>> Stashed changes
)
