package com.micrantha.eyespie.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun CameraLayout(
    onBack: () -> Unit,
    onCaptured: (CapturedImage) -> Unit,
    onCameraError: (Throwable) -> Unit,
    onAvailabilityChanged: (CameraAvailability) -> Unit,
    busy: Boolean,
    captureButton: @Composable ColumnScope.(capture: () -> Unit) -> Unit,
    recoveryMessage: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        CameraCapture(
            modifier = Modifier.fillMaxSize(),
            onAvailabilityChanged = onAvailabilityChanged,
            onCameraError = onCameraError,
            onCaptured = onCaptured,
            captureButton = { capture ->
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    EyespieTopBar(
                        onBack = onBack,
                        backContentDescription = "Back",
                    )

                    content()

                    Spacer(modifier = Modifier.weight(1f))

                    captureButton(capture)
                }
            },
            recoveryButton = { openSettings ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
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
                            onClick = openSettings,
                        )
                        EyespieSecondaryAction(
                            text = "Back",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onBack,
                        )
                    }
                }
            },
        )
    }
}
