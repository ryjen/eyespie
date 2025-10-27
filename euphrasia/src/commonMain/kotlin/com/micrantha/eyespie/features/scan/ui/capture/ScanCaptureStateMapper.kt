package com.micrantha.eyespie.features.scan.ui.capture

import com.micrantha.bluebell.arch.StateMapper
import com.micrantha.eyespie.features.scan.entities.ScanState
import com.micrantha.eyespie.features.scan.entities.ScanUiState

class ScanCaptureStateMapper : StateMapper<ScanState, ScanUiState> {

    override fun map(state: ScanState): ScanUiState =
        ScanUiState(
            enabled = state.enabled,
            busy = state.busy,
        )
}
