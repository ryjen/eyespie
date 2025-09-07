package com.micrantha.eyespie.features.scan.ui.capture

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import com.micrantha.eyespie.domain.entities.ColorProof
import com.micrantha.eyespie.domain.entities.DetectProof
import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.LabelProof
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlinx.coroutines.flow.Flow
import okio.Path

@Stable
data class ScanState(
    val labels: LabelProof? = null,
    val location: Location? = null,
    val colors: ColorProof? = null,
    val image: CameraImage? = null,
    val obfuscated: CameraImage? = null,
    val enabled: Boolean = true,
    val busy: Boolean = false,
    val match: Embedding? = null,
    val detection: DetectProof? = null,
    val path: Path? = null,
    val playerID: String? = null,
)

@Stable
data class ScanUiState(
    val clues: List<String>,
    val enabled: Boolean,
    val busy: Boolean,
    val capture: Painter?
)

sealed interface ScanAction {
    interface ScanSavable {
        val path: Path
    }

    data object SaveScan : ScanAction

    data object EditScan : ScanAction

    data object SaveError : ScanAction

    data class GeneratedImage(val image: CameraImage) : ScanAction

    data class Loaded(val data: Flow<CameraImage>) : ScanAction

    data class EditSaved(override val path: Path) : ScanAction, ScanSavable
    data class ImageSaved(override val path: Path) : ScanAction, ScanSavable

    data object Back : ScanAction
}
