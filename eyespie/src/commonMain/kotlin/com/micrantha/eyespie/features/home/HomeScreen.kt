package com.micrantha.eyespie.features.home

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
fun HomeScreen(
    state: HomeState,
    dispatch: (HomeIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.failure?.let { failure ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(localGameFailureMessage(failure), modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { dispatch(HomeIntent.DismissFailure) }) { Text("Dismiss") }
            }
        }
        if (state.loading && state.content == null) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
            return@Column
        }
        state.content?.let { content ->
            Text("Local agent: ${content.identityDisplayName}", style = MaterialTheme.typography.titleSmall)
            Text("Identity ${content.identityIdSuffix}", style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { dispatch(HomeIntent.CreateSelected) }) { Text("Create game") }
            OutlinedButton(onClick = { dispatch(HomeIntent.OnboardingSelected) }) { Text("How to play") }
            OutlinedButton(onClick = { dispatch(HomeIntent.Refresh) }) { Text("Refresh") }
        }
        HorizontalDivider()
        Text("Local games", style = MaterialTheme.typography.titleLarge)
        val games = state.content?.games.orEmpty()
        if (games.isEmpty()) Text("No games yet. Create a target and clue entirely on this device.")
        games.forEach { game ->
            Text(game.name, style = MaterialTheme.typography.titleMedium)
            if (game.things.isEmpty()) Text("No playable targets in this game.")
            game.things.forEach { thing ->
                Text(thing.clueText, style = MaterialTheme.typography.bodyLarge)
                if (thing.bestSimilarity != null) {
                    Text(
                        if (thing.matched) "Matched · best ${formatSimilarity(thing.bestSimilarity)}"
                        else "Best ${formatSimilarity(thing.bestSimilarity)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedButton(
                    onClick = { dispatch(HomeIntent.PlaySelected(game.id, thing.id)) },
                ) { Text("Play") }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun formatSimilarity(value: Double?): String {
    if (value == null) return "—"
    val percentageTenths = (value * 1000.0).toInt()
    return "${percentageTenths / 10.0}%"
}
