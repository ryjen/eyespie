package com.micrantha.eyespie.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.imaging.CameraAvailability
import com.micrantha.eyespie.imaging.CameraCapture
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.presentation.theme.EyespieEyebrow
import com.micrantha.eyespie.presentation.theme.EyespiePanel
import com.micrantha.eyespie.presentation.theme.EyespieSecondaryAction
import com.micrantha.eyespie.presentation.theme.EyespieTopBar
import com.micrantha.eyespie.telemetry.LocalOperationalTelemetry

/**
 * Presentation-only override used by deterministic visual tests. Production callers receive the
 * real platform [CameraCapture] because the default value is null.
 */
internal typealias CameraCaptureSurfaceOverride = @Composable (
    modifier: Modifier,
    onCaptured: (CapturedImage) -> Unit,
    captureOverlay: @Composable ((capture: () -> Unit) -> Unit),
) -> Unit

internal val LocalCameraCaptureSurfaceOverride =
    staticCompositionLocalOf<CameraCaptureSurfaceOverride?> { null }

@Composable
fun CameraLayout(
    onBack: () -> Unit,
    onCaptured: (CapturedImage) -> Unit,
    onCameraError: (Throwable) -> Unit,
    onAvailabilityChanged: (CameraAvailability) -> Unit,
    busy: Boolean,
    captureButton: @Composable ColumnScope.(capture: () -> Unit) -> Unit,
    recoveryMessage: String,
    backLabel: String = "Back",
    modifier: Modifier = Modifier,
    edgeToEdgeControls: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val telemetry = LocalOperationalTelemetry.current
    val availabilityDiagnostics = remember(telemetry) {
        CameraAvailabilityDiagnosticObserver(telemetry)
    }

    var captureInProgress by remember { mutableStateOf(false) }
    val captureCompleted: (CapturedImage) -> Unit = { image ->
        captureInProgress = false
        onCaptured(image)
    }
    val captureFailed: (Throwable) -> Unit = { error ->
        captureInProgress = false
        onCameraError(error)
    }

    Box(modifier = modifier.fillMaxSize()) {
        val captureOverlay: @Composable ((capture: () -> Unit) -> Unit) = { capture ->
            var overlayModifier = Modifier.fillMaxSize()
            if (edgeToEdgeControls) {
                overlayModifier = overlayModifier
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp)
            }
            overlayModifier = overlayModifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)

            Column(
                modifier = overlayModifier,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                EyespieTopBar(
                    onBack = onBack,
                    backContentDescription = backLabel,
                    backEnabled = !busy,
                    backChevronOnly = true,
                )

                content()

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        captureButton {
                            if (!captureInProgress) {
                                // Immediate feedback; native image conversion completes later.
                                captureInProgress = true
                                capture()
                            }
                        }
                    }
                    if (captureInProgress) {
                        Row(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                    MaterialTheme.shapes.small,
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Capturing…", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        val surfaceOverride = LocalCameraCaptureSurfaceOverride.current
        if (surfaceOverride != null) {
            surfaceOverride(Modifier.fillMaxSize(), captureCompleted, captureOverlay)
        } else {
            CameraCapture(
                modifier = Modifier.fillMaxSize(),
                onAvailabilityChanged = { availability ->
                    availabilityDiagnostics.onAvailabilityChanged(availability)
                    onAvailabilityChanged(availability)
                },
                onCameraError = captureFailed,
                onCaptured = captureCompleted,
                captureButton = captureOverlay,
                recoveryButton = { openSettings ->
                    var recoveryModifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                    if (edgeToEdgeControls) {
                        recoveryModifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(16.dp)
                    }
                    Box(
                        modifier = recoveryModifier,
                        contentAlignment = Alignment.Center,
                    ) {
                        EyespiePanel(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        ) {
                            EyespieEyebrow("Camera access")
                            Text(
                                recoveryMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            EyespieSecondaryAction(
                                text = "Open camera settings",
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy,
                                onClick = openSettings,
                            )
                            EyespieSecondaryAction(
                                text = backLabel,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy,
                                onClick = onBack,
                            )
                        }
                    }
                },
            )
        }
    }
}
