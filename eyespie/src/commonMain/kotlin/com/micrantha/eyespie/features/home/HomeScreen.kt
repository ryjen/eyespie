package com.micrantha.eyespie.features.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.presentation.ThumbnailOrAvatar
import com.micrantha.eyespie.presentation.localGameFailureMessage
import com.micrantha.eyespie.presentation.theme.EyespieEyebrow
import com.micrantha.eyespie.presentation.theme.EyespieLogo
import com.micrantha.eyespie.presentation.theme.EyespiePanel
import com.micrantha.eyespie.presentation.theme.EyespiePrimaryAction
import com.micrantha.eyespie.presentation.theme.EyespieSecondaryAction
import com.micrantha.eyespie.presentation.theme.EyespieSectionHeader
import com.micrantha.eyespie.presentation.theme.EyespieStatusBadge
import com.micrantha.eyespie.presentation.theme.extendedColors

@Composable
fun HomeScreen(
    state: HomeState,
    dispatch: (HomeIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HomeHeader(state)
        LocalModeRow()

        state.failure?.let { failure ->
            MessageCard(
                message = localGameFailureMessage(failure),
                actionLabel = "Dismiss",
                onAction = { dispatch(HomeIntent.DismissFailure) },
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

        state.importPreview?.let { preview ->
            ImportPreviewCard(
                preview = preview,
                adding = state.importInProgress,
                onConfirm = { dispatch(HomeIntent.ImportConfirmed) },
                onCancel = { dispatch(HomeIntent.ImportPreviewCancelled) },
            )
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EyespieSectionHeader(
                title = "Your games",
                supportingText = "Stored on this device",
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(onClick = { dispatch(HomeIntent.CreateSelected) }) {
                Icon(Icons.Default.Add, contentDescription = "Create game")
            }
        }

        val games = state.content?.games.orEmpty()
        if (games.isEmpty()) {
            EmptyGamesCard()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                games.forEach { game ->
                    GameCard(
                        game = game,
                        thumbnails = state.content?.thumbnails?.get(game.id.value),
                        onOpen = { dispatch(HomeIntent.GameSelected(game.id)) },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EyespiePrimaryAction(
                text = "Create game",
                modifier = Modifier.fillMaxWidth(),
                onClick = { dispatch(HomeIntent.CreateSelected) },
            )
            EyespieSecondaryAction(
                text = if (state.importInProgress) "Importing…" else "Import .eyespie",
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.importInProgress,
                onClick = { dispatch(HomeIntent.ImportSelected) },
            )
        }

        EyespieSecondaryAction(
            text = "Profile, settings & help",
            modifier = Modifier.fillMaxWidth(),
            onClick = { dispatch(HomeIntent.UtilitySelected) },
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HomeHeader(state: HomeState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EyespieLogo(size = 42.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            EyespieEyebrow("Eyespie · local field desk")
            Text(
                "Field desk",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            state.content?.let { content ->
                Text(
                    "Agent ${content.identityDisplayName} · ${content.identityIdSuffix}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LocalModeRow() {
    val colors = extendedColors
    Surface(
        color = colors.successContainer,
        contentColor = colors.onSuccessContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, colors.success.copy(alpha = 0.24f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = colors.success,
                contentColor = colors.onSuccess,
                shape = CircleShape,
                modifier = Modifier.size(30.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Local Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "No hosted account · authority stays on this device",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ImportPreviewCard(
    preview: HomeImportPreview,
    adding: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.size(104.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(48.dp))
            }
        }

        EyespiePanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EyespieEyebrow("Verified game file")
                EyespieStatusBadge("SIGNED")
            }
            Text(preview.gameName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "${preview.clueCount} ${if (preview.clueCount == 1) "clue" else "clues"}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Creator …${preview.creatorIdSuffix}\nGame …${preview.gameIdSuffix}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Signature valid. Adding this game stores the playable case on this device. The signature proves integrity and creator-key continuity; it does not make the bundle secret.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        EyespiePrimaryAction(
            text = if (adding) "Adding…" else "Add game",
            modifier = Modifier.fillMaxWidth(),
            enabled = !adding,
            onClick = onConfirm,
        )
        EyespieSecondaryAction(
            text = "Cancel",
            modifier = Modifier.fillMaxWidth(),
            enabled = !adding,
            onClick = onCancel,
        )
    }
}

@Composable
private fun GameCard(
    game: HomeGame,
    thumbnails: Map<ThingId, ByteArray>?,
    onOpen: () -> Unit,
) {
    val matched = game.things.count { it.matched }
    val total = game.things.size
    val progress = if (total == 0) "No clues yet" else "$matched of $total clues found"
    val role = if (game.localCreator) "Created here" else "Shared game"
    val cover = game.things.firstOrNull()?.let { thumbnails?.get(it.id) }

    Card(
        onClick = onOpen,
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
                modifier = Modifier.size(54.dp),
            ) {
                ThumbnailOrAvatar(
                    thumbnail = cover,
                    modifier = Modifier.fillMaxSize(),
                    avatar = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(27.dp)) },
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(game.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(progress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                EyespieStatusBadge(role)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun EmptyGamesCard() {
    EyespiePanel {
        EyespieEyebrow("Open a case")
        Text("No field cases yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Create a game on this device, or join one another player shared with you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MessageCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    EyespiePanel(containerColor = MaterialTheme.colorScheme.errorContainer) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        EyespieSecondaryAction(
            text = actionLabel,
            modifier = Modifier.fillMaxWidth(),
            onClick = onAction,
        )
    }
}
