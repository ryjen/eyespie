package com.micrantha.eyespie.features.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.failure?.let { failure ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        if (state.loading && state.content == null) {
            CircularProgressIndicator()
            OutlinedButton(onClick = { dispatch(PlayGameIntent.Back) }) { Text("Back") }
            return@Column
        }

        val content = state.content
        if (content == null) {
            Text("This local game is no longer available.")
            OutlinedButton(onClick = { dispatch(PlayGameIntent.Back) }) { Text("Back") }
            return@Column
        }

        Text(content.gameName, style = MaterialTheme.typography.titleLarge)
        Text("Clue", style = MaterialTheme.typography.titleSmall)
        Text(content.clueText, style = MaterialTheme.typography.headlineSmall)

        val outcome = state.latestOutcome
        val matched = outcome?.progress?.matched ?: content.matched
        val bestSimilarity = outcome?.progress?.bestSimilarity ?: content.bestSimilarity
        if (bestSimilarity != null) {
            Text(
                if (matched) "Progress: matched · best ${formatSimilarity(bestSimilarity)}"
                else "Progress: best ${formatSimilarity(bestSimilarity)}",
            )
        }
        outcome?.let {
            Text(
                if (it.match.matched) "Match · similarity ${formatSimilarity(it.match.similarity)}"
                else "Not a match · similarity ${formatSimilarity(it.match.similarity)}",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        CameraCapture(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            onCameraError = { dispatch(PlayGameIntent.CameraFailed) },
            onCaptured = { dispatch(PlayGameIntent.GuessCaptured(it)) },
            captureButton = { capture ->
                Button(onClick = capture, enabled = !state.busy) {
                    Text(if (state.busy) "Matching…" else "Capture guess")
                }
            },
        )
        OutlinedButton(onClick = { dispatch(PlayGameIntent.Back) }, enabled = !state.busy) { Text("Back") }
    }
}

private fun formatSimilarity(value: Double): String {
    val percentageTenths = (value * 1000.0).toInt()
    return "${percentageTenths / 10.0}%"
}
