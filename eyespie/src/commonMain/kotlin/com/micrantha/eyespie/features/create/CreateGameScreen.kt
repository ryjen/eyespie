package com.micrantha.eyespie.features.create

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.micrantha.eyespie.imaging.CameraAvailability
import com.micrantha.eyespie.presentation.AuthoringCaptureLayout
import com.micrantha.eyespie.presentation.cameraUnavailableMessage
import com.micrantha.eyespie.presentation.localGameFailureMessage
import com.micrantha.eyespie.presentation.targetCameraPermissionMessage
import com.micrantha.eyespie.presentation.theme.EyespieEyebrow
import com.micrantha.eyespie.presentation.theme.EyespiePanel
import com.micrantha.eyespie.presentation.theme.EyespiePrimaryAction
import com.micrantha.eyespie.presentation.theme.EyespieSecondaryAction

@Composable
fun CreateGameScreen(
    state: CreateGameState,
    dispatch: (CreateGameIntent) -> Unit,
) {
    AuthoringCaptureLayout(
        onBack = { dispatch(CreateGameIntent.Back) },
        onCommit = { dispatch(CreateGameIntent.TargetCaptured(it)) },
        onCameraError = { dispatch(CreateGameIntent.CameraFailed) },
        onAvailabilityChanged = { availability ->
            if (availability == CameraAvailability.Unavailable) {
                dispatch(CreateGameIntent.CameraFailed)
            }
        },
        busy = state.busy,
        recoveryMessage = targetCameraPermissionMessage(),
        backLabel = "Back to field desk",
        captureLabel = "Capture target",
        liveContent = {
            if (state.failure == CreateGameFailure.CameraUnavailable) {
                EyespiePanel(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    EyespieEyebrow("Camera unavailable", color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(
                        cameraUnavailableMessage(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    EyespieSecondaryAction(
                        text = "Dismiss",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { dispatch(CreateGameIntent.DismissFailure) },
                    )
                }
            }
        },
    ) { onRetake, onCommit ->
        (state.failure as? CreateGameFailure.Game)?.let { failure ->
            EyespiePanel(containerColor = MaterialTheme.colorScheme.errorContainer) {
                EyespieEyebrow("Could not create case", color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    localGameFailureMessage(failure.failure),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                EyespieSecondaryAction(
                    text = "Dismiss",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { dispatch(CreateGameIntent.DismissFailure) },
                )
            }
        }

        EyespiePanel(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ) {
            EyespieEyebrow("New field case")
            Text(
                "Build the first clue",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Use the captured target as context, then write the briefing the player will receive.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.name,
                onValueChange = { dispatch(CreateGameIntent.NameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Game name") },
                enabled = !state.busy,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            OutlinedTextField(
                value = state.clue,
                onValueChange = { dispatch(CreateGameIntent.ClueChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Clue for the player") },
                supportingText = { Text("Describe what they should look for without giving the target away.") },
                enabled = !state.busy,
                shape = MaterialTheme.shapes.medium,
            )
            OutlinedTextField(
                value = state.expectedAnswer,
                onValueChange = { dispatch(CreateGameIntent.ExpectedAnswerChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Expected answer") },
                supportingText = { Text("Creator-only authority. This answer is not included in shared playable data.") },
                enabled = !state.busy,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            EyespiePrimaryAction(
                text = if (state.busy) "Creating…" else "Create game",
                modifier = Modifier.fillMaxWidth(),
                onClick = onCommit,
                enabled = !state.busy &&
                    state.name.isNotBlank() &&
                    state.clue.isNotBlank() &&
                    state.expectedAnswer.isNotBlank(),
            )
            EyespieSecondaryAction(
                text = "Retake target",
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.busy,
                onClick = {
                    dispatch(CreateGameIntent.DismissFailure)
                    onRetake()
                },
            )
        }
    }
}
