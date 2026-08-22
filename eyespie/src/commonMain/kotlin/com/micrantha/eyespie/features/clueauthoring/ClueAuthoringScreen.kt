package com.micrantha.eyespie.features.clueauthoring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.imaging.CameraCapture
import com.micrantha.eyespie.presentation.localGameFailureMessage

@Composable
fun ClueAuthoringScreen(
    state: ClueAuthoringState,
    dispatch: (ClueAuthoringIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.failure?.let { failure ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when (failure) {
                        ClueAuthoringFailure.CameraUnavailable -> "Camera access is unavailable. Check permission/settings and try again."
                        is ClueAuthoringFailure.Game -> localGameFailureMessage(failure.failure)
                    },
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { dispatch(ClueAuthoringIntent.DismissFailure) }) { Text("Dismiss") }
            }
        }

        Text("Add clue", style = MaterialTheme.typography.titleLarge)
        Text("The target image is converted to an on-device embedding. The original target image is not stored as game authority.")
        OutlinedTextField(
            value = state.clue,
            onValueChange = { dispatch(ClueAuthoringIntent.ClueChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Clue") },
            enabled = !state.busy,
        )
        OutlinedTextField(
            value = state.expectedAnswer,
            onValueChange = { dispatch(ClueAuthoringIntent.ExpectedAnswerChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Expected answer (creator-only)") },
            enabled = !state.busy,
            singleLine = true,
        )
        CameraCapture(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            onCameraError = { dispatch(ClueAuthoringIntent.CameraFailed) },
            onCaptured = { dispatch(ClueAuthoringIntent.TargetCaptured(it)) },
            captureButton = { capture ->
                Button(
                    onClick = capture,
                    enabled = !state.busy && state.clue.isNotBlank() && state.expectedAnswer.isNotBlank(),
                ) { Text(if (state.busy) "Adding…" else "Capture target & add clue") }
            },
        )
        OutlinedButton(
            onClick = { dispatch(ClueAuthoringIntent.Back) },
            enabled = !state.busy,
        ) { Text("Back to game") }
    }
}
