package com.micrantha.eyespie.features.create

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
fun CreateGameScreen(
    state: CreateGameState,
    dispatch: (CreateGameIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.failure?.let { failure ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when (failure) {
                        CreateGameFailure.CameraUnavailable -> "Camera access is unavailable. Check permission/settings and try again."
                        is CreateGameFailure.Game -> localGameFailureMessage(failure.failure)
                    },
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { dispatch(CreateGameIntent.DismissFailure) }) { Text("Dismiss") }
            }
        }
        Text("Create local game", style = MaterialTheme.typography.titleLarge)
        Text("The target image is used to derive an embedding; it is not saved as game authority.")
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
            label = { Text("Clue") },
            enabled = !state.busy,
        )
        OutlinedTextField(
            value = state.expectedAnswer,
            onValueChange = { dispatch(CreateGameIntent.ExpectedAnswerChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Expected answer (creator-only)") },
            enabled = !state.busy,
            singleLine = true,
        )
        CameraCapture(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            onCameraError = { dispatch(CreateGameIntent.CameraFailed) },
            onCaptured = { dispatch(CreateGameIntent.TargetCaptured(it)) },
            captureButton = { capture ->
                Button(
                    onClick = capture,
                    enabled = !state.busy && state.name.isNotBlank() && state.clue.isNotBlank() && state.expectedAnswer.isNotBlank(),
                ) { Text(if (state.busy) "Creating…" else "Capture target & create") }
            },
        )
        OutlinedButton(onClick = { dispatch(CreateGameIntent.Back) }, enabled = !state.busy) { Text("Back") }
    }
}
