package com.micrantha.eyespie.features.scan.entities

import com.micrantha.bluebell.domain.StateMap
import com.micrantha.eyespie.domain.entities.AiProof
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.platform.scan.CameraImage
import okio.Path

data class ScanEditState(
    val image: CameraImage? = null,
    val clues: AiProof? = null,
    val selected: StateMap<Int, ScanClue>? = null,
    val embedding: Embedding? = null,
    val name: String? = null,
    val disabled: Boolean = false,
    val location: Location? = null,
    val path: Path? = null,
    val isBusy: Boolean = true,
    val isError: Boolean = false,
    val hasSelected: Boolean = false,
)
