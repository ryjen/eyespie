package com.micrantha.eyespie.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
                )

                content()

                Spacer(modifier = Modifier.weight(1f))

                captureButton(capture)
            }
        }

        val surfaceOverride = LocalCameraCaptureSurfaceOverride.current
        if (surfaceOverride != null) {
            surfaceOverride(Modifier.fillMaxSize(), onCaptured, captureOverlay)
        } else {
            CameraCapture(
                modifier = Modifier.fillMaxSize(),
                onAvailabilityChanged = onAvailabilityChanged,
                onCameraError = onCameraError,
                onCaptured = onCaptured,
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
