package com.micrantha.eyespie.features.scan.entities

import com.micrantha.bluebell.domain.StateMap
import com.micrantha.eyespie.domain.ai.GeneratedClues
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.platform.scan.CameraImage
import okio.Path

enum class ClueAuthoringMode {
    CHOOSE,
    GENERATED,
    MANUAL,
}

data class ScanEditState(
    val image: CameraImage? = null,
    val clues: GeneratedClues? = null,
    val selected: StateMap<Int, ScanClue>? = null,
    val authoringMode: ClueAuthoringMode = ClueAuthoringMode.CHOOSE,
    val manualClue: String = "",
    val manualAnswer: String = "",
    val generationAvailable: Boolean = false,
    val generationUnavailable: Boolean = false,
    val embedding: Embedding? = null,
    val name: String? = null,
    val disabled: Boolean = false,
    val location: Location? = null,
    val path: Path? = null,
    val isBusy: Boolean = true,
    val isError: Boolean = false,
    val hasSelected: Boolean = false,
)
