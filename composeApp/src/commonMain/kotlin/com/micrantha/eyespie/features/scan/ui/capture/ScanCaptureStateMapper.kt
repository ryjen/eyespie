package com.micrantha.eyespie.features.scan.ui.capture

import androidx.compose.ui.graphics.painter.BitmapPainter
import com.micrantha.bluebell.app.Log
import com.micrantha.bluebell.arch.StateMapper

class ScanCaptureStateMapper : StateMapper<ScanState, ScanUiState> {

    override fun map(state: ScanState): ScanUiState = try {
        ScanUiState(
            enabled = state.enabled,
            busy = state.busy,
            capture = if (state.enabled.not()) state.image?.toImageBitmap()?.let {
                BitmapPainter(it)
            } else null
        )
    } catch (err: Throwable) {
        Log.e("mapping", err) { "unable to map scan state" }
        throw err
    }
}
