package com.micrantha.eyespie.features.scan.entities

import androidx.compose.runtime.Stable
import com.micrantha.eyespie.domain.entities.Location

@Stable
data class ScanState(
    val enabled: Boolean = true,
    val busy: Boolean = false,
    val location: Location? = null,
)
