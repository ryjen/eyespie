package com.micrantha.eyespie.features.scan.entities

import androidx.compose.runtime.Stable

@Stable
data class ScanUiState(
    val enabled: Boolean,
    val busy: Boolean,
)
