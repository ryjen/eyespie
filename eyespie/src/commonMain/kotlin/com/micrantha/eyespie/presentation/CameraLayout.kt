package com.micrantha.eyespie.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.imaging.CameraAvailability
import com.micrantha.eyespie.imaging.CameraCapture
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.presentation.theme.EyespieLogo

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
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onBack,
                            enabled = !busy,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            ),
                        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                        EyespieLogo(size = 32.dp)
                    }

                    content()

                    Spacer(modifier = Modifier.weight(1f))

                    captureButton(capture)
                }
            },
            recoveryButton = { openSettings ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(recoveryMessage)
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = openSettings,
                            ) { Text("Open camera settings") }
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onBack,
                            ) { Text("Back") }
                        }
                    }
                }
            },
        )
    }
}
