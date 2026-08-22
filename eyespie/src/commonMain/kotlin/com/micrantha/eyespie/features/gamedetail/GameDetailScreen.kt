package com.micrantha.eyespie.features.gamedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.presentation.localGameFailureMessage

@Composable
fun GameDetailScreen(
    state: GameDetailState,
    dispatch: (GameDetailIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(onClick = { dispatch(GameDetailIntent.Back) }) { Text("Back to games") }

        state.failure?.let { failure ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(localGameFailureMessage(failure), modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { dispatch(GameDetailIntent.DismissFailure) }) { Text("Dismiss") }
            }
        }
        state.shareResult?.let { result ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(gameDetailShareMessage(result), modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { dispatch(GameDetailIntent.DismissShareResult) }) { Text("Dismiss") }
            }
        }

        if (state.loading && state.content == null) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
            return@Column
        }

        val content = state.content ?: return@Column
        Text(content.name, style = MaterialTheme.typography.headlineMedium)

        val found = content.things.count { it.matched }
        Text(
            if (content.things.isEmpty()) "No clues yet"
            else "$found of ${content.things.size} clues found",
            style = MaterialTheme.typography.titleMedium,
        )
        if (content.localCreator) {
            Button(onClick = { dispatch(GameDetailIntent.AddClueSelected) }) {
                Text("Add clue")
            }
            OutlinedButton(
                enabled = !state.shareInProgress,
                onClick = { dispatch(GameDetailIntent.ShareSelected) },
            ) {
                Text(if (state.shareInProgress) "Sharing…" else "Share game")
            }
        }
        HorizontalDivider()

        if (content.things.isEmpty()) {
            Text("This game does not have a playable clue yet.")
        }

        content.things.forEachIndexed { index, thing ->
            Text("Clue ${index + 1}", style = MaterialTheme.typography.labelLarge)
            Text(thing.clueText, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (thing.matched) "Found" else "Searching",
                style = MaterialTheme.typography.bodyMedium,
            )
            thing.bestSimilarity?.let { similarity ->
                Text("Best match ${formatSimilarity(similarity)}", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { dispatch(GameDetailIntent.PlaySelected(thing.id)) }) {
                Text(if (thing.matched) "View clue" else "Play clue")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun gameDetailShareMessage(result: GameDetailShareResult): String = when (result) {
    GameDetailShareResult.Shared -> "Game shared."
    GameDetailShareResult.NotLocalCreator -> "Only games authored by this local identity can be shared."
    GameDetailShareResult.TooLarge -> "The Eyespie game is too large to share."
    GameDetailShareResult.Busy -> "Another document operation is already running."
    GameDetailShareResult.Failed -> "This local game could not be shared."
    GameDetailShareResult.Unavailable -> "Game sharing is unavailable on this platform."
    GameDetailShareResult.Cancelled -> ""
}

private fun formatSimilarity(value: Double): String {
    val percentageTenths = (value * 1000.0).toInt()
    return "${percentageTenths / 10.0}%"
}
