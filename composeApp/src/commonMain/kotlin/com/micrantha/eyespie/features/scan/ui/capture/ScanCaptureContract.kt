package com.micrantha.eyespie.features.scan.ui.capture

import androidx.compose.runtime.Stable
import com.micrantha.eyespie.domain.entities.Location
import com.micrantha.eyespie.platform.scan.CameraImage
import okio.Path

@Stable
data class ScanState(
    val enabled: Boolean = true,
    val busy: Boolean = false,
    val location: Location? = null,
)

@Stable
data class ScanUiState(
    val enabled: Boolean,
    val busy: Boolean,
)

sealed interface ScanAction {
    data class SaveScan(
        val image: CameraImage,
        val path: Path
    ) : ScanAction

    data object ScanError : ScanAction

    data object Back : ScanAction
}
