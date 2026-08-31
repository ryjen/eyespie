package com.micrantha.eyespie.features.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.imaging.CameraAvailability
import com.micrantha.eyespie.imaging.CameraCapture
import com.micrantha.eyespie.presentation.cameraUnavailableMessage
import com.micrantha.eyespie.presentation.localGameFailureMessage
import com.micrantha.eyespie.presentation.targetCameraPermissionMessage
import com.micrantha.eyespie.presentation.theme.EyespieLogo

@Composable
fun CreateGameScreen(
    state: CreateGameState,
    dispatch: (CreateGameIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { dispatch(CreateGameIntent.Back) },
                enabled = !state.busy,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to field desk") }
            EyespieLogo(size = 32.dp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "NEW FIELD CASE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text("Create a game", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Set the clue, choose the answer, then capture the real-world target.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.failure?.let { failure ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        when (failure) {
                            CreateGameFailure.CameraUnavailable -> cameraUnavailableMessage()
                            is CreateGameFailure.Game -> localGameFailureMessage(failure.failure)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { dispatch(CreateGameIntent.DismissFailure) }) { Text("Dismiss") }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Case briefing", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { dispatch(CreateGameIntent.NameChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Game name") },
                    enabled = !state.busy,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.clue,
                    onValueChange = { dispatch(CreateGameIntent.ClueChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Clue for the player") },
                    supportingText = { Text("Describe what they should look for without giving the target away.") },
                    enabled = !state.busy,
                )
                OutlinedTextField(
                    value = state.expectedAnswer,
                    onValueChange = { dispatch(CreateGameIntent.ExpectedAnswerChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Expected answer") },
                    supportingText = { Text("Creator-only authority. This answer is not included in shared playable data.") },
                    enabled = !state.busy,
                    singleLine = true,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Capture target", style = MaterialTheme.typography.titleLarge)
                Text(
                    "The image is used on this device to derive the target embedding. The original target image is not portable game authority.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CameraCapture(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    onAvailabilityChanged = { availability ->
                        if (availability == CameraAvailability.Unavailable) {
                            dispatch(CreateGameIntent.CameraFailed)
                        }
                    },
                    onCameraError = { dispatch(CreateGameIntent.CameraFailed) },
                    onCaptured = { dispatch(CreateGameIntent.TargetCaptured(it)) },
                    captureButton = { capture ->
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = capture,
                            enabled = !state.busy && state.name.isNotBlank() && state.clue.isNotBlank() && state.expectedAnswer.isNotBlank(),
                        ) { Text(if (state.busy) "Creating…" else "Capture target & create game") }
                    },
                    recoveryButton = { openSettings ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(targetCameraPermissionMessage())
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = openSettings,
                            ) { Text("Open camera settings") }
                        }
                    },
                )
            }
        }
    }
}
