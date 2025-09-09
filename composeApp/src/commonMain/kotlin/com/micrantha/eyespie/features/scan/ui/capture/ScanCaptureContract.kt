package com.micrantha.eyespie.features.scan.ui.capture

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import com.micrantha.eyespie.platform.scan.CameraImage
import kotlinx.coroutines.flow.Flow
import okio.Path

@Stable
data class ScanState(
    val image: CameraImage? = null,
    val enabled: Boolean = true,
    val busy: Boolean = false,
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

    data object DoneScan : ScanAction

    data object ScanError : ScanAction

    data class Loaded(val data: Flow<CameraImage>) : ScanAction

    data object Back : ScanAction
}
