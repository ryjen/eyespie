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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.imaging.CameraCapture
import com.micrantha.eyespie.presentation.localGameFailureMessage

@Composable
fun CreateGameScreen(
    state: CreateGameState,
    dispatch: (CreateGameIntent) -> Unit,
) {
    val addingClue = state.mode is CreateGameMode.AddClue

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(
            onClick = { dispatch(CreateGameIntent.Back) },
            enabled = !state.busy,
        ) {
            Text(if (addingClue) "Back to game" else "Back to field desk")
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (addingClue) "NEW CLUE" else "NEW FIELD CASE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(if (addingClue) "Add a clue" else "Create a game", style = MaterialTheme.typography.headlineLarge)
            Text(
                if (addingClue) {
                    "Author the clue and creator-only answer, then capture the real-world target."
                } else {
                    "Set the clue, choose the answer, then capture the real-world target."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.failure?.let { failure ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(if (addingClue) "Clue briefing" else "Case briefing", style = MaterialTheme.typography.titleLarge)
                if (!addingClue) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { dispatch(CreateGameIntent.NameChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Game name") },
                        enabled = !state.busy,
                        singleLine = true,
                    )
                }
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
                    onCameraError = { dispatch(CreateGameIntent.CameraFailed) },
                    onCaptured = { dispatch(CreateGameIntent.TargetCaptured(it)) },
                    captureButton = { capture ->
                        val ready = state.clue.isNotBlank() &&
                            state.expectedAnswer.isNotBlank() &&
                            (addingClue || state.name.isNotBlank())
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = capture,
                            enabled = !state.busy && ready,
                        ) {
                            Text(
                                when {
                                    state.busy -> "Saving…"
                                    addingClue -> "Capture target & add clue"
                                    else -> "Capture target & create game"
                                },
                            )
                        }
                    },
                )
            }
        }
    }
}
