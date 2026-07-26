package com.micrantha.eyespie.features.scan.ui.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.micrantha.eyespie.core.PreviewContext
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAction
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.scan.entities.ScanUiState

@Preview(showBackground = true, backgroundColor = 0xFF, widthDp = 200, heightDp = 400)
@Composable
fun ScanCapturePreview() = PreviewContext(
    ScanUiState(
        enabled = true,
        busy = false,
        cameraAuthorizationLoaded = true,
        cameraReady = false,
        cameraAuthorization = CapabilityAuthorization.NotRequested,
        cameraRequestInFlight = false,
        cameraAction = CapabilityAction.Request,
        cameraActionLabel = "Allow camera",
        cameraStatus = "Camera access has not been requested.",
    ),
) {
    ScanCaptureScreen()
}
