package com.micrantha.eyespie.features.scan.entities

import com.micrantha.eyespie.domain.entities.AiClue
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.platform.scan.CameraImage
import okio.Path

data class ScanEditState(
    val image: CameraImage? = null,
    val clues: MutableSet<AiClue>? = null,
    val selected: MutableSet<Int>? = null,
    val embedding: Embedding? = null,
    val name: String? = null,
    val disabled: Boolean = false,
    val location: Location? = null,
    val path: Path? = null,
    val isBusy: Boolean = true
)
