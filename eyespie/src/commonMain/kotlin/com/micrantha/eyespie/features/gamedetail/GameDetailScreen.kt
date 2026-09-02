package com.micrantha.eyespie.features.gamedetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.presentation.ThumbnailOrAvatar
import com.micrantha.eyespie.presentation.localGameFailureMessage
import com.micrantha.eyespie.presentation.theme.EyespieEyebrow
import com.micrantha.eyespie.presentation.theme.EyespieHeader
import com.micrantha.eyespie.presentation.theme.EyespiePanel
import com.micrantha.eyespie.presentation.theme.EyespiePrimaryAction
import com.micrantha.eyespie.presentation.theme.EyespieSecondaryAction
import com.micrantha.eyespie.presentation.theme.EyespieSectionHeader
import com.micrantha.eyespie.presentation.theme.EyespieStatusBadge
import com.micrantha.eyespie.presentation.theme.EyespieTopBar
import com.micrantha.eyespie.presentation.theme.extendedColors

@Composable
fun GameDetailScreen(
    state: GameDetailState,
    dispatch: (GameDetailIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        EyespieTopBar(
            onBack = { dispatch(GameDetailIntent.Back) },
            backContentDescription = "Back to field desk",
        )

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
            ) { CircularProgressIndicator() }
            return@Column
        }

        val content = state.content ?: return@Column
        val found = content.things.count { it.matched }
        val total = content.things.size

        EyespieHeader(
            eyebrow = if (content.localCreator) "Your field case" else "Shared field case",
            title = content.name,
            subtitle = if (total == 0) "No playable clues yet" else "$found of $total clues found",
        )
        if (total > 0) {
            LinearProgressIndicator(
                progress = { found.toFloat() / total },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer,
            )
        }

        EyespieSectionHeader(
            title = "Clues",
            supportingText = if (content.localCreator) "Review and test this case." else "Work through the case one clue at a time.",
        )

        if (content.things.isEmpty()) {
            EyespiePanel {
                Text(
                    if (content.localCreator) "This case does not have a playable clue yet." else "This shared case has no playable clues.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            content.things.forEachIndexed { index, thing ->
                ClueCard(
                    index = index,
                    thing = thing,
                    onPlay = { dispatch(GameDetailIntent.PlaySelected(thing.id)) },
                )
            }
        }

        if (content.localCreator) {
            EyespieSectionHeader(
                title = "Case tools",
                supportingText = "Author locally or export a signed handoff.",
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EyespiePrimaryAction(
                    text = "Add clue",
                    modifier = Modifier.weight(1f),
                    onClick = { dispatch(GameDetailIntent.AddClueSelected) },
                )
                EyespieSecondaryAction(
                    text = if (state.shareInProgress) "Preparing…" else "Share game",
                    modifier = Modifier.weight(1f),
                    enabled = !state.shareInProgress,
                    onClick = { dispatch(GameDetailIntent.ShareSelected) },
                )
            }
            Text(
                "Share exports a signed .eyespie file through the platform handoff flow.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
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
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(58.dp),
            ) {
                ThumbnailOrAvatar(
                    thumbnail = thing.thumbnail,
                    modifier = Modifier.fillMaxSize(),
                    avatar = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(28.dp)) },
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EyespieEyebrow("Clue ${index + 1}")
                    EyespieStatusBadge(
                        text = if (thing.matched) "Found" else "In progress",
                        containerColor = if (thing.matched) colors.successContainer else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (thing.matched) colors.onSuccessContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Text(thing.clueText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                thing.bestSimilarity?.let { similarity ->
                    Text(
                        "Best match ${formatSimilarity(similarity)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageCard(
    message: String,
    onDismiss: () -> Unit,
) {
    EyespiePanel(containerColor = MaterialTheme.colorScheme.errorContainer) {
        EyespieEyebrow("Case unavailable", color = MaterialTheme.colorScheme.onErrorContainer)
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
        EyespieSecondaryAction(
            text = "Dismiss",
            modifier = Modifier.fillMaxWidth(),
            onClick = onDismiss,
        )
    }
}

private fun formatSimilarity(value: Double): String {
    val percentageTenths = (value * 1000.0).toInt()
    return "${percentageTenths / 10.0}%"
}
