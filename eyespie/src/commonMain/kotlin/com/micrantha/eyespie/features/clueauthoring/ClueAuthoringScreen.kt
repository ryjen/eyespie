package com.micrantha.eyespie.features.clueauthoring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.imaging.CameraAvailability
import com.micrantha.eyespie.presentation.CameraLayout
import com.micrantha.eyespie.presentation.cameraUnavailableMessage
import com.micrantha.eyespie.presentation.clueTargetCameraPermissionMessage
import com.micrantha.eyespie.presentation.localGameFailureMessage
import com.micrantha.eyespie.presentation.theme.EyespieLogo

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
            Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                onClick = {
                    dispatch(ClueAuthoringIntent.CaptureStarted)
                    capture()
                },
                enabled = !state.busy && state.clue.isNotBlank(),
            ) { Text(if (state.busy) "Processing…" else "Capture target & add clue") }
        },
    ) {
        state.failure?.let { failure ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        when (failure) {
                            ClueAuthoringFailure.CameraUnavailable -> cameraUnavailableMessage()
                            is ClueAuthoringFailure.Game -> localGameFailureMessage(failure.failure)
                        },
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    OutlinedButton(onClick = { dispatch(ClueAuthoringIntent.DismissFailure) }) { Text("Dismiss") }
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
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "ADD A CLUE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = state.clue,
                    onValueChange = { dispatch(ClueAuthoringIntent.ClueChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Clue") },
                    supportingText = { Text("Required") },
                    enabled = !state.busy,
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                )
            }
        }
    }
}
