package com.micrantha.eyespie.features.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.imaging.CameraCapture
import com.micrantha.eyespie.presentation.localGameFailureMessage

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
        ) {
            Text("Back to field case")
        }

        state.failure?.let { failure ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        when (failure) {
                            PlayGameFailure.CameraUnavailable -> "Camera access is unavailable. Check permission/settings and try again."
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
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val content = state.content
        if (content == null) {
            Text("This local game is no longer available.")
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "ACTIVE FIELD CASE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(content.gameName, style = MaterialTheme.typography.headlineLarge)
            Text(
                "${content.foundCount} of ${content.totalCount} clues found",
                style = MaterialTheme.typography.bodyLarge,
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
                content.bestSimilarity?.let { similarity ->
                    Text(
                        "Best match ${formatSimilarity(similarity)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        when (val feedback = state.feedback) {
            is PlayFeedback.Matched -> FoundCard(
                feedback = feedback,
                onContinue = { dispatch(PlayGameIntent.Continue) },
            )
            is PlayFeedback.Mismatch -> MismatchCard(feedback)
            null -> if (content.matched) {
                FoundCard(
                    feedback = PlayFeedback.Matched(
                        similarity = content.bestSimilarity ?: 1.0,
                        bestSimilarity = content.bestSimilarity ?: 1.0,
                        foundCount = content.foundCount,
                        totalCount = content.totalCount,
                        nextThingId = content.nextThingId,
                    ),
                    onContinue = { dispatch(PlayGameIntent.Continue) },
                    restored = true,
                )
            }
        }

        val matched = state.feedback is PlayFeedback.Matched || content.matched
        if (!matched) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Check the scene", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (state.feedback is PlayFeedback.Mismatch) {
                            "That wasn't close enough. Reframe the target and try another capture."
                        } else {
                            "Point the camera at what you think matches the clue, then capture a guess."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CameraCapture(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        onCameraError = { dispatch(PlayGameIntent.CameraFailed) },
                        onCaptured = { dispatch(PlayGameIntent.GuessCaptured(it)) },
                        captureButton = { capture ->
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = capture,
                                enabled = !state.busy,
                            ) {
                                Text(if (state.busy) "Checking…" else if (state.feedback is PlayFeedback.Mismatch) "Try another guess" else "Capture guess")
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FoundCard(
    feedback: PlayFeedback.Matched,
    onContinue: () -> Unit,
    restored: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (feedback.completed) "Field case complete" else "Clue found",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                when {
                    feedback.completed -> "All clues in this game have been found."
                    restored -> "This clue was already found on this device. Continue with the next open clue."
                    else -> "Match confirmed at ${formatSimilarity(feedback.similarity)}. Progress is saved on this device."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "${feedback.foundCount} of ${feedback.totalCount} clues found",
                style = MaterialTheme.typography.labelLarge,
            )
            Button(modifier = Modifier.fillMaxWidth(), onClick = onContinue) {
                Text(if (feedback.completed) "Return to field case" else "Next clue")
            }
        }
    }
}

@Composable
private fun MismatchCard(feedback: PlayFeedback.Mismatch) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Not a match yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Similarity ${formatSimilarity(feedback.similarity)} · best ${formatSimilarity(feedback.bestSimilarity)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun formatSimilarity(value: Double): String {
    val percentageTenths = (value * 1000.0).toInt()
    return "${percentageTenths / 10.0}%"
}
