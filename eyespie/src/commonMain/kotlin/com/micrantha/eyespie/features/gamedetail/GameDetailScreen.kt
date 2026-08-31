package com.micrantha.eyespie.features.gamedetail

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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.presentation.ThumbnailOrAvatar
import com.micrantha.eyespie.presentation.theme.extendedColors
import com.micrantha.eyespie.presentation.localGameFailureMessage
import com.micrantha.eyespie.presentation.theme.EyespieLogo

@Composable
fun GameDetailScreen(
    state: GameDetailState,
    dispatch: (GameDetailIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { dispatch(GameDetailIntent.Back) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to field desk")
            }
            EyespieLogo(size = 32.dp)
        }

        state.failure?.let { failure ->
            MessageCard(
                message = localGameFailureMessage(failure),
                onDismiss = { dispatch(GameDetailIntent.DismissFailure) },
            )
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

        val content = state.content ?: return@Column
        val found = content.things.count { it.matched }
        val total = content.things.size

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (content.localCreator) "YOUR FIELD CASE" else "SHARED FIELD CASE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(content.name, style = MaterialTheme.typography.headlineLarge)
            Text(
                if (total == 0) "No playable clues yet" else "$found of $total clues found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (content.localCreator) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Build this case", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Add another clue and capture its real-world target on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { dispatch(GameDetailIntent.AddClueSelected) },
                    ) {
                        Text("Add clue")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Share this game", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Export a signed .eyespie game file and hand it off with the platform share flow.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.shareInProgress,
                        onClick = { dispatch(GameDetailIntent.ShareSelected) },
                    ) {
                        Text(if (state.shareInProgress) "Preparing…" else "Share game")
                    }
                }
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Clues", style = MaterialTheme.typography.headlineSmall)
            Text(
                if (content.localCreator) "Review your case or continue testing clues." else "Work through the case one clue at a time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (content.things.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Text(
                    if (content.localCreator) "This case does not have a playable clue yet." else "This shared case has no playable clues.",
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                )
            }
        }

        content.things.forEachIndexed { index, thing ->
            ClueCard(
                index = index,
                thing = thing,
                onPlay = { dispatch(GameDetailIntent.PlaySelected(thing.id)) },
            )
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ClueCard(
    index: Int,
    thing: GameDetailThing,
    onPlay: () -> Unit,
) {
    val colors = extendedColors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Clue ${index + 1}", style = MaterialTheme.typography.labelLarge)
                Surface(
                    color = if (thing.matched) colors.successContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (thing.matched) colors.onSuccessContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (thing.matched) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            if (thing.matched) "Found" else "In progress",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(56.dp),
                ) {
                    ThumbnailOrAvatar(
                        thumbnail = thing.thumbnail,
                        modifier = Modifier.fillMaxSize(),
                        avatar = {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(28.dp))
                        },
                    )
                }
                Text(thing.clueText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            thing.bestSimilarity?.let { similarity ->
                Text(
                    "Best match ${formatSimilarity(similarity)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onPlay,
            ) {
                Text(if (thing.matched) "Review clue" else "Start clue")
            }
        }
    }
}

@Composable
private fun MessageCard(
    message: String,
    onDismiss: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

private fun formatSimilarity(value: Double): String {
    val percentageTenths = (value * 1000.0).toInt()
    return "${percentageTenths / 10.0}%"
}
