package com.micrantha.eyespie.features.scan.entities

import com.micrantha.eyespie.domain.entities.Location
import okio.Path

data class ScanEditParams(
    val image: Path,
    val location: Location
)
