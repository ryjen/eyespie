package com.micrantha.eyespie.features.scan.entities

import androidx.compose.ui.graphics.painter.Painter

data class ScanEditUiState(
    val clues: List<Clue>,
    val image: Painter?,
    val isBusy: Boolean,
    val enabled: Boolean
) {
    data class Clue(
        val answer: String,
        val clue: String,
        val isSelected: Boolean,
    )
}
