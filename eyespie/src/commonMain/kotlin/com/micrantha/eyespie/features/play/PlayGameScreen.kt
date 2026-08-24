package com.micrantha.eyespie.features.play

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.imaging.CameraAvailability
import com.micrantha.eyespie.imaging.CameraCapture
import com.micrantha.eyespie.presentation.cameraUnavailableMessage
import com.micrantha.eyespie.presentation.localGameFailureMessage
import com.micrantha.eyespie.presentation.playCameraPermissionMessage

@Composable
fun PlayGameScreen(
    state: PlayGameState,
    dispatch: (PlayGameIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(
            onClick = { dispatch(PlayGameIntent.Back) },
            enabled = !state.busy,
        ) { Text("Back to game") }

        state.failure?.let { failure ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        when (failure) {
                            PlayGameFailure.CameraUnavailable -> cameraUnavailableMessage()
                            is PlayGameFailure.Game -> localGameFailureMessage(failure.failure)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { dispatch(PlayGameIntent.DismissFailure) }) { Text("Dismiss") }
                }
            }
        }

        if (state.loading && state.content == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Column
        }

        val content = state.content
        if (content == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Case unavailable", style = MaterialTheme.typography.titleLarge)
                    Text("This local game or clue is no longer available on this device.")
                }
            }
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "FIELD CASE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(content.gameName, style = MaterialTheme.typography.headlineLarge)
            Text(
                "Clue ${content.clueNumber} of ${content.clueCount} · ${state.matchedClues} found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Your clue", style = MaterialTheme.typography.labelLarge)
                Text(content.clueText, style = MaterialTheme.typography.headlineSmall)
            }
        }

        when {
            state.completed -> CompletionCard(onBack = { dispatch(PlayGameIntent.Back) })
            state.matched -> FoundCard(
                hasNext = content.nextThingId != null,
                onNext = { dispatch(PlayGameIntent.NextClueSelected) },
                onBack = { dispatch(PlayGameIntent.Back) },
            )
            else -> {
                state.latestOutcome?.let { outcome ->
                    if (!outcome.match.matched) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text("Not it yet", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Try another angle or move closer to the object described by the clue.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
                        Text("Scan your guess", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Frame the object clearly, then capture one still image to check it on this device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        CameraCapture(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            onAvailabilityChanged = { availability ->
                                if (availability == CameraAvailability.Unavailable) {
                                    dispatch(PlayGameIntent.CameraFailed)
                                }
                            },
                            onCameraError = { dispatch(PlayGameIntent.CameraFailed) },
                            onCaptured = { dispatch(PlayGameIntent.GuessCaptured(it)) },
                            captureButton = { capture ->
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = capture,
                                    enabled = !state.busy,
                                ) { Text(if (state.busy) "Checking…" else "Check this object") }
                            },
                            recoveryButton = { openSettings ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(playCameraPermissionMessage())
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = openSettings,
                                    ) { Text("Open camera settings") }
                                }
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun FoundCard(
    hasNext: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Clue found", style = MaterialTheme.typography.headlineSmall)
            Text("The match has been saved to this device.")
            if (hasNext) {
                Button(modifier = Modifier.fillMaxWidth(), onClick = onNext) { Text("Next clue") }
            } else {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onBack) { Text("Back to game") }
            }
        }
    }
}

@Composable
private fun CompletionCard(onBack: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Case complete", style = MaterialTheme.typography.headlineSmall)
            Text("Every clue in this game has been found. Progress is stored on this device.")
            Button(modifier = Modifier.fillMaxWidth(), onClick = onBack) { Text("Back to game") }
        }
    }
}
