package com.micrantha.eyespie.features.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.imaging.CameraAvailability
import com.micrantha.eyespie.presentation.CameraLayout
import com.micrantha.eyespie.presentation.cameraUnavailableMessage
import com.micrantha.eyespie.presentation.localGameFailureMessage
import com.micrantha.eyespie.presentation.playCameraPermissionMessage
import com.micrantha.eyespie.presentation.theme.EyespieLogo
import com.micrantha.eyespie.presentation.theme.extendedColors

@Composable
fun PlayGameScreen(
    state: PlayGameState,
    dispatch: (PlayGameIntent) -> Unit,
) {
    if (state.loading && state.content == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
        return
    }

    val content = state.content
    if (content == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { dispatch(PlayGameIntent.Back) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                EyespieLogo(size = 32.dp)
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Case unavailable", style = MaterialTheme.typography.titleLarge)
                    Text("This local game or clue is no longer available on this device.")
                }
            }
        }
        return
    }

    CameraLayout(
        onBack = { dispatch(PlayGameIntent.Back) },
        onCaptured = { dispatch(PlayGameIntent.GuessCaptured(it)) },
        onCameraError = { dispatch(PlayGameIntent.CameraFailed) },
        onAvailabilityChanged = { availability ->
            if (availability == CameraAvailability.Unavailable) {
                dispatch(PlayGameIntent.CameraFailed)
            }
        },
        busy = state.busy,
        recoveryMessage = playCameraPermissionMessage(),
        captureButton = { capture ->
            if (!state.completed && !state.matched) {
                Button(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    onClick = capture,
                    enabled = !state.busy,
                ) { Text(if (state.busy) "Checking…" else "Check this object") }
            }
        },
    ) {
        state.failure?.let { failure ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
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
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    OutlinedButton(onClick = { dispatch(PlayGameIntent.DismissFailure) }) { Text("Dismiss") }
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
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "FIELD CASE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(content.gameName, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Clue ${content.clueNumber} of ${content.clueCount} · ${state.matchedClues} found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val progress = if (content.clueCount == 0) 0f else state.matchedClues.toFloat() / content.clueCount
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            ),
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
            }
        }
    }
}

@Composable
private fun FoundCard(
    hasNext: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = extendedColors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.successContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(28.dp))
                Text("Clue found", style = MaterialTheme.typography.headlineSmall)
            }
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
    val colors = extendedColors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.successContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(28.dp))
                Text("Case complete", style = MaterialTheme.typography.headlineSmall)
            }
            Text("Every clue in this game has been found. Progress is stored on this device.")
            Button(modifier = Modifier.fillMaxWidth(), onClick = onBack) { Text("Back to game") }
        }
    }
}
