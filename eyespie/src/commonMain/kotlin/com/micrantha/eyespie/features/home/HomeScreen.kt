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
        if (games.isEmpty()) {
            Text("No games yet. Create a target and clue entirely on this device.")
        }
        games.forEach { game ->
            Text(game.name, style = MaterialTheme.typography.titleMedium)
            val matched = game.things.count { it.matched }
            Text(
                if (game.things.isEmpty()) "No playable clues yet"
                else "$matched of ${game.things.size} clues found",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = { dispatch(HomeIntent.GameSelected(game.id)) }) {
                Text("Open game")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
