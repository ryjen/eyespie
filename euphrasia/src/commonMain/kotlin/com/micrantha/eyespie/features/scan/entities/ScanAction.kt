package com.micrantha.eyespie.features.scan.entities

import com.micrantha.eyespie.platform.scan.CameraImage
import okio.Path

sealed interface ScanAction {
    data class SaveScan(
        val image: CameraImage,
        val path: Path
    ) : ScanAction

    data object ScanError : ScanAction

    data object Back : ScanAction
}
