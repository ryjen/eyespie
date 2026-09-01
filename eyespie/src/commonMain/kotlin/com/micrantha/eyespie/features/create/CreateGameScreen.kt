package com.micrantha.eyespie.features.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.micrantha.eyespie.presentation.CameraLayout
import com.micrantha.eyespie.presentation.cameraUnavailableMessage
import com.micrantha.eyespie.presentation.localGameFailureMessage
import com.micrantha.eyespie.presentation.targetCameraPermissionMessage
import com.micrantha.eyespie.presentation.theme.EyespieLogo

@Composable
fun CreateGameScreen(
    state: CreateGameState,
    dispatch: (CreateGameIntent) -> Unit,
) {
    CameraLayout(
        onBack = { dispatch(CreateGameIntent.Back) },
        onCaptured = { dispatch(CreateGameIntent.TargetCaptured(it)) },
        onCameraError = { dispatch(CreateGameIntent.CameraFailed) },
        onAvailabilityChanged = { availability ->
            if (availability == CameraAvailability.Unavailable) {
                dispatch(CreateGameIntent.CameraFailed)
            }
        },
        busy = state.busy,
        recoveryMessage = targetCameraPermissionMessage(),
        captureButton = { capture ->
            Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                onClick = capture,
                enabled = !state.busy && state.name.isNotBlank() && state.clue.isNotBlank() && state.expectedAnswer.isNotBlank(),
            ) { Text(if (state.busy) "Creating…" else "Capture target & create game") }
        },
    ) {
        state.failure?.let { failure ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
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
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    OutlinedButton(onClick = { dispatch(CreateGameIntent.DismissFailure) }) { Text("Dismiss") }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "NEW FIELD CASE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
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
    }
}
