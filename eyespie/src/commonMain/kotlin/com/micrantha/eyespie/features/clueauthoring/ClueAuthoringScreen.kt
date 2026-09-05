package com.micrantha.eyespie.features.clueauthoring

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.micrantha.eyespie.imaging.CameraAvailability
import com.micrantha.eyespie.presentation.AuthoringCaptureLayout
import com.micrantha.eyespie.presentation.cameraUnavailableMessage
import com.micrantha.eyespie.presentation.clueTargetCameraPermissionMessage
import com.micrantha.eyespie.presentation.localGameFailureMessage
import com.micrantha.eyespie.presentation.theme.EyespieEyebrow
import com.micrantha.eyespie.presentation.theme.EyespiePanel
import com.micrantha.eyespie.presentation.theme.EyespiePrimaryAction
import com.micrantha.eyespie.presentation.theme.EyespieSecondaryAction

@Composable
fun ClueAuthoringScreen(
    state: ClueAuthoringState,
    dispatch: (ClueAuthoringIntent) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    AuthoringCaptureLayout(
        onBack = { dispatch(ClueAuthoringIntent.Back) },
        onCommit = { dispatch(ClueAuthoringIntent.TargetCaptured(it)) },
        onCameraError = { dispatch(ClueAuthoringIntent.CameraFailed) },
        onAvailabilityChanged = { availability ->
            if (availability == CameraAvailability.Unavailable) {
                dispatch(ClueAuthoringIntent.CameraFailed)
            }
        },
        busy = state.busy,
        recoveryMessage = clueTargetCameraPermissionMessage(),
        backLabel = "Back to game",
        captureLabel = "Capture clue target",
        liveContent = {
            if (state.failure == ClueAuthoringFailure.CameraUnavailable) {
                EyespiePanel(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    EyespieEyebrow("Camera unavailable", color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(
                        cameraUnavailableMessage(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    EyespieSecondaryAction(
                        text = "Dismiss",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { dispatch(ClueAuthoringIntent.DismissFailure) },
                    )
                }
            }
        },
    ) { onRetake, onCommit ->
        (state.failure as? ClueAuthoringFailure.Game)?.let { failure ->
            EyespiePanel(containerColor = MaterialTheme.colorScheme.errorContainer) {
                EyespieEyebrow("Could not add clue", color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    localGameFailureMessage(failure.failure),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                EyespieSecondaryAction(
                    text = "Dismiss",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { dispatch(ClueAuthoringIntent.DismissFailure) },
                )
            }
        }

        EyespiePanel(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ) {
            EyespieEyebrow("Add a clue")
            Text(
                "Write the field note",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Use the captured target as context. The player receives the clue; the expected answer remains creator-only authority.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.clue,
                onValueChange = { dispatch(ClueAuthoringIntent.ClueChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Clue") },
                supportingText = { Text("Required") },
                enabled = !state.busy,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            )
            OutlinedTextField(
                value = state.expectedAnswer,
                onValueChange = { dispatch(ClueAuthoringIntent.ExpectedAnswerChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Expected answer (creator-only)") },
                supportingText = { Text("Required") },
                enabled = !state.busy,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            )
            EyespiePrimaryAction(
                text = if (state.busy) "Adding clue…" else "Add clue",
                modifier = Modifier.fillMaxWidth(),
                onClick = onCommit,
                enabled = !state.busy && state.clue.isNotBlank() && state.expectedAnswer.isNotBlank(),
            )
            EyespieSecondaryAction(
                text = "Retake target",
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.busy,
                onClick = {
                    dispatch(ClueAuthoringIntent.DismissFailure)
                    onRetake()
                },
            )
        }
    }
}
