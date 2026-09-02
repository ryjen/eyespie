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
import com.micrantha.eyespie.presentation.CameraLayout
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

    CameraLayout(
        onBack = { dispatch(ClueAuthoringIntent.Back) },
        onCaptured = { dispatch(ClueAuthoringIntent.TargetCaptured(it)) },
        onCameraError = { dispatch(ClueAuthoringIntent.CameraFailed) },
        onAvailabilityChanged = { availability ->
            if (availability == CameraAvailability.Unavailable) {
                dispatch(ClueAuthoringIntent.CameraFailed)
            }
        },
        busy = state.busy,
        recoveryMessage = clueTargetCameraPermissionMessage(),
        captureButton = { capture ->
            EyespiePrimaryAction(
                text = if (state.busy) "Processing…" else "Capture target & add clue",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    dispatch(ClueAuthoringIntent.CaptureStarted)
                    capture()
                },
                enabled = !state.busy && state.clue.isNotBlank(),
            )
        },
    ) {
        state.failure?.let { failure ->
            EyespiePanel(containerColor = MaterialTheme.colorScheme.errorContainer) {
                EyespieEyebrow("Could not add clue", color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    when (failure) {
                        ClueAuthoringFailure.CameraUnavailable -> cameraUnavailableMessage()
                        is ClueAuthoringFailure.Game -> localGameFailureMessage(failure.failure)
                    },
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
                "The player receives the clue. The expected answer remains creator-only authority.",
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
        }
    }
}
