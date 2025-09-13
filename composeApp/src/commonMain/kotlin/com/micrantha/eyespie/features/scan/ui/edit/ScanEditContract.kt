package com.micrantha.eyespie.features.scan.ui.edit

import androidx.compose.ui.graphics.painter.Painter
import com.micrantha.eyespie.core.ui.component.Choice
import com.micrantha.eyespie.domain.entities.ColorClue
import com.micrantha.eyespie.domain.entities.DetectClue
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.LabelClue
import com.micrantha.eyespie.domain.entities.Location
import okio.Path

data class ScanEditParams(
    val image: Path,
    val location: Location
)

data class ScanEditState(
    val labels: MutableMap<String, LabelClue>? = null,
    val customLabel: String? = null,
    val colors: MutableMap<String, ColorClue>? = null,
    val customColor: String? = null,
    val detections: MutableMap<String, DetectClue>? = null,
    val customDetection: String? = null,
    val embedding: Embedding? = null,
    val name: String? = null,
    val image: Painter? = null,
    val disabled: Boolean = false,
    val location: Location? = null,
    val path: Path? = null
)

data class ScanEditUiState(
    val labels: List<Choice>,
    val customLabel: String?,
    val colors: List<Choice>,
    val customColor: String?,
    val detections: List<Choice>,
    val customDetection: String?,
    val name: String,
    val image: Painter?,
    val enabled: Boolean
)

sealed interface ScanEditAction {
    data class Init(val params: ScanEditParams) : ScanEditAction

    data class LabelChanged(val data: Choice) : ScanEditAction
    data class ColorChanged(val data: Choice) : ScanEditAction

    data class CustomLabelChanged(val data: String) : ScanEditAction

    data object SaveScanEdit : ScanEditAction

    data object SaveThingError : ScanEditAction

    data object LoadError : ScanEditAction

    data class LoadedImage(val data: Painter) : ScanEditAction

    data class NameChanged(val data: String) : ScanEditAction

    data object ClearColor : ScanEditAction

    data object ClearLabel : ScanEditAction
}
