package com.micrantha.eyespie.features.scan.ui.capture

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.platform.scan.CameraImage

@Stable
data class ScanState(
    val image: CameraImage? = null,
    val enabled: Boolean = true,
    val busy: Boolean = false,
    val location: Location? = null,
)

@Stable
data class ScanUiState(
    val enabled: Boolean,
    val busy: Boolean,
    val capture: Painter?
)

sealed interface ScanAction {
    data object SaveScan : ScanAction

    data object ScanError : ScanAction

    data object Back : ScanAction
}
