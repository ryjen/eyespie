package com.micrantha.eyespie.features.create

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.micrantha.eyespie.imaging.CameraAvailability
import com.micrantha.eyespie.presentation.CameraLayout
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
            EyespiePrimaryAction(
                text = if (state.busy) "Creating…" else "Capture target & create game",
                modifier = Modifier.fillMaxWidth(),
                onClick = capture,
                enabled = !state.busy && state.name.isNotBlank() && state.clue.isNotBlank() && state.expectedAnswer.isNotBlank(),
            )
        },
    ) {
        state.failure?.let { failure ->
            EyespiePanel(containerColor = MaterialTheme.colorScheme.errorContainer) {
                EyespieEyebrow("Could not create case", color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    when (failure) {
                        CreateGameFailure.CameraUnavailable -> cameraUnavailableMessage()
                        is CreateGameFailure.Game -> localGameFailureMessage(failure.failure)
                    },
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
                "Write the briefing first, then frame the real-world target in the camera.",
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
        }
    }
}
