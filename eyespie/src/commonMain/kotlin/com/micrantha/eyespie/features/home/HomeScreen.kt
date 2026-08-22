package com.micrantha.eyespie.features.home

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.presentation.localGameFailureMessage

@Composable
fun HomeScreen(
    state: HomeState,
    dispatch: (HomeIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HomeHeader(state)

        state.failure?.let { failure ->
            MessageCard(
                message = localGameFailureMessage(failure),
                actionLabel = "Dismiss",
                onAction = { dispatch(HomeIntent.DismissFailure) },
            )
        }
        state.importResult?.let { result ->
            homeImportMessage(result).takeIf { it.isNotBlank() }?.let { message ->
                MessageCard(
                    message = message,
                    actionLabel = "Dismiss",
                    onAction = { dispatch(HomeIntent.DismissImportResult) },
                )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { dispatch(HomeIntent.CreateSelected) },
            ) {
                Text("Create game")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = !state.importInProgress,
                onClick = { dispatch(HomeIntent.ImportSelected) },
            ) {
                Text(if (state.importInProgress) "Importing…" else "Join game")
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Your games", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Stored on this device. Share or join with a signed .eyespie file.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val games = state.content?.games.orEmpty()
        if (games.isEmpty()) {
            EmptyGamesCard(
                onCreate = { dispatch(HomeIntent.CreateSelected) },
                onImport = { dispatch(HomeIntent.ImportSelected) },
                importEnabled = !state.importInProgress,
            )
        } else {
            games.forEach { game ->
                GameCard(game = game, onOpen = { dispatch(HomeIntent.GameSelected(game.id)) })
            }
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { dispatch(HomeIntent.UtilitySelected) },
        ) {
            Text("Profile, settings & help")
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun HomeHeader(state: HomeState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "EYESPIE",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text("Field desk", style = MaterialTheme.typography.headlineLarge)
        state.content?.let { content ->
            Text(
                "Agent ${content.identityDisplayName} · ${content.identityIdSuffix}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GameCard(
    game: HomeGame,
    onOpen: () -> Unit,
) {
    val matched = game.things.count { it.matched }
    val total = game.things.size
    val progress = if (total == 0) "No clues yet" else "$matched of $total clues found"
    val role = if (game.localCreator) "Created here" else "Shared game"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(game.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(
                    role,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                progress,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
                Text(if (total > 0 && matched < total) "Continue" else "Open game")
            }
        }
    }
}

@Composable
private fun EmptyGamesCard(
    onCreate: () -> Unit,
    onImport: () -> Unit,
    importEnabled: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("No field cases yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Create a game on this device, or join one another player shared with you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(modifier = Modifier.fillMaxWidth(), onClick = onCreate) {
                Text("Create your first game")
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = importEnabled,
                onClick = onImport,
            ) {
                Text("Import .eyespie")
            }
        }
    }
}

@Composable
private fun MessageCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

private fun homeImportMessage(result: HomeImportResult): String = when (result) {
    HomeImportResult.Imported -> "Game imported."
    HomeImportResult.AlreadyPresent -> "Game already present."
    HomeImportResult.Conflict -> "A different local game already uses this game ID. Your existing game was not changed."
    HomeImportResult.InvalidFile -> "The selected file is not a supported Eyespie game."
    HomeImportResult.TooLarge -> "The selected Eyespie file is too large."
    HomeImportResult.Busy -> "Another document operation is already running."
    HomeImportResult.Failed -> "The selected game could not be verified or imported."
    HomeImportResult.Unavailable -> "Game import is unavailable on this platform."
    HomeImportResult.Cancelled -> ""
}
