package com.micrantha.eyespie.features.scan.entities

import androidx.compose.ui.graphics.painter.Painter

data class ScanEditUiState(
    val clues: Collection<ScanClue>,
    val image: Painter?,
    val isBusy: Boolean,
    val enabled: Boolean,
    val isError: Boolean
)
