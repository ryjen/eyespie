package com.micrantha.eyespie.features.clueauthoring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.imaging.CameraAvailability
import com.micrantha.eyespie.imaging.CameraCapture
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
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EyespieLogo(size = 32.dp)
        }

        state.failure?.let { failure ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when (failure) {
                        ClueAuthoringFailure.CameraUnavailable -> cameraUnavailableMessage()
                        is ClueAuthoringFailure.Game -> localGameFailureMessage(failure.failure)
                    },
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { dispatch(ClueAuthoringIntent.DismissFailure) }) { Text("Dismiss") }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "ADD A CLUE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text("Author a clue", style = MaterialTheme.typography.headlineLarge)
            Text("Write the player-facing clue, set the creator-only answer, then capture the real-world target.")
        }
        OutlinedTextField(
            value = state.clue,
            onValueChange = { dispatch(ClueAuthoringIntent.ClueChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Clue") },
            enabled = !state.busy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { keyboardController?.hide() }),
        )
        OutlinedTextField(
            value = state.expectedAnswer,
            onValueChange = { dispatch(ClueAuthoringIntent.ExpectedAnswerChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Expected answer (creator-only)") },
            enabled = !state.busy,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        )
        CameraCapture(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            onAvailabilityChanged = { availability ->
                if (availability == CameraAvailability.Unavailable) {
                    dispatch(ClueAuthoringIntent.CameraFailed)
                }
            },
            onCameraError = { dispatch(ClueAuthoringIntent.CameraFailed) },
            onCaptured = { dispatch(ClueAuthoringIntent.TargetCaptured(it)) },
            captureButton = { capture ->
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = capture,
                    enabled = !state.busy,
                ) { Text(if (state.busy) "Adding…" else "Capture target & add clue") }
            },
            recoveryButton = { openSettings ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(clueTargetCameraPermissionMessage())
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = openSettings,
                    ) { Text("Open camera settings") }
                }
            },
        )
        OutlinedButton(
            onClick = { dispatch(ClueAuthoringIntent.Back) },
            enabled = !state.busy,
        ) { Text("Back to game") }
    }
}
