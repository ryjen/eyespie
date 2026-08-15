package com.micrantha.eyespie.features.scan.entities

import com.micrantha.bluebell.platform.Serializable
import com.micrantha.eyespie.domain.entities.Location

data class ScanEditParams(
    val image: String,
    val location: Location
) : Serializable
