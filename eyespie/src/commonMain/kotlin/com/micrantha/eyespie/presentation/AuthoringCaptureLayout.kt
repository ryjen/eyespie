package com.micrantha.eyespie.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.imaging.CameraAvailability
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.presentation.theme.EyespieTopBar

/**
 * Route-local two-phase authoring surface.
 *
 * The live camera is composed only until a still is captured. The captured image is then retained
 * only in this bounded composition while the creator reviews it and fills the form. It never enters
 * feature StateFlow, navigation state, persistence, or the shared playable game model.
 */
@Composable
fun AuthoringCaptureLayout(
    onBack: () -> Unit,
    onCommit: (CapturedImage) -> Unit,
    onCameraError: (Throwable) -> Unit,
    onAvailabilityChanged: (CameraAvailability) -> Unit,
    busy: Boolean,
    recoveryMessage: String,
    backLabel: String,
    captureLabel: String,
    modifier: Modifier = Modifier,
    liveContent: @Composable ColumnScope.() -> Unit = {},
    reviewContent: @Composable ColumnScope.(onRetake: () -> Unit, onCommit: () -> Unit) -> Unit,
) {
    var capturedImage by remember { mutableStateOf<CapturedImage?>(null) }
    val captured = capturedImage

    if (captured == null) {
        CameraLayout(
            onBack = onBack,
            onCaptured = { capturedImage = it },
            onCameraError = onCameraError,
            onAvailabilityChanged = onAvailabilityChanged,
            busy = busy,
            recoveryMessage = recoveryMessage,
            backLabel = backLabel,
            modifier = modifier,
            captureButton = { capture ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilledIconButton(
                        onClick = capture,
                        enabled = !busy,
                        modifier = Modifier
                            .size(72.dp)
                            .semantics { contentDescription = captureLabel },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .border(3.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                        )
                    }
                    Text(
                        captureLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
            content = liveContent,
        )
        return
    }

    val bitmap = remember(captured) { decodeThumbnail(captured.encodedBytes()) }
    var formVisible by remember(captured) { mutableStateOf(false) }
    LaunchedEffect(captured) { formVisible = true }

    Box(modifier = modifier.fillMaxSize()) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.18f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            EyespieTopBar(
                onBack = onBack,
                backContentDescription = backLabel,
                backEnabled = !busy,
            )

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(
                visible = formVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                exit = fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    reviewContent(
                        {
                            if (!busy) capturedImage = null
                        },
                        {
                            if (!busy) onCommit(captured)
                        },
                    )
                }
            }
        }
    }
}
