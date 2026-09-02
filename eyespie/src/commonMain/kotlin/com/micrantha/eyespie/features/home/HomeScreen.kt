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
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        HomeHeader(state)
        LocalModeCard()

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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            EyespiePrimaryAction(
                text = "Create game",
                modifier = Modifier.weight(1f),
                onClick = { dispatch(HomeIntent.CreateSelected) },
            )
            EyespieSecondaryAction(
                text = if (state.importInProgress) "Importing…" else "Import .eyespie",
                modifier = Modifier.weight(1f),
                enabled = !state.importInProgress,
                onClick = { dispatch(HomeIntent.ImportSelected) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EyespieSectionHeader(
                title = "Your games",
                supportingText = "Stored on this device. Share or join with a signed .eyespie file.",
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(
                onClick = { dispatch(HomeIntent.CreateSelected) },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create game")
            }
        }

        val games = state.content?.games.orEmpty()
        if (games.isEmpty()) {
            EmptyGamesCard(
                onCreate = { dispatch(HomeIntent.CreateSelected) },
                onImport = { dispatch(HomeIntent.ImportSelected) },
                importEnabled = !state.importInProgress,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                games.forEach { game ->
                    GameCard(
                        game = game,
                        thumbnails = state.content?.thumbnails?.get(game.id.value),
                        onOpen = { dispatch(HomeIntent.GameSelected(game.id)) },
                    )
                }
            }
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
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            EyespieEyebrow("Eyespie · local field desk")
            Text(
                "Field desk",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            state.content?.let { content ->
                Text(
                    "Agent ${content.identityDisplayName} · ${content.identityIdSuffix}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        EyespieLogo(size = 52.dp)
    }
}

@Composable
private fun LocalModeCard() {
    val colors = extendedColors
    Surface(
        color = colors.successContainer,
        contentColor = colors.onSuccessContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, colors.success.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = colors.success,
                contentColor = colors.onSuccess,
                shape = CircleShape,
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Local Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "No hosted account required · game authority stays on this device",
                    style = MaterialTheme.typography.bodyMedium,
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
    EyespiePanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EyespieEyebrow("Verified game file")
            EyespieStatusBadge("SIGNED")
        }
        Text(preview.gameName, style = MaterialTheme.typography.headlineSmall)
        Text(
            "${preview.clueCount} ${if (preview.clueCount == 1) "clue" else "clues"}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Creator …${preview.creatorIdSuffix} · Game …${preview.gameIdSuffix}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Eyespie verified the bundle format, creator identity, and signature. Adding it stores the playable game on this device; signed bundles provide integrity, not secrecy, and their gameplay data can be inspected by a device owner.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(56.dp),
            ) {
                ThumbnailOrAvatar(
                    thumbnail = cover,
                    modifier = Modifier.fillMaxSize(),
                    avatar = {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(28.dp))
                    },
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(game.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    progress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
private fun EmptyGamesCard(
    onCreate: () -> Unit,
    onImport: () -> Unit,
    importEnabled: Boolean,
) {
    EyespiePanel {
        EyespieEyebrow("Open a case")
        Text("No field cases yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Create a game on this device, or join one another player shared with you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EyespiePrimaryAction(
            text = "Create your first game",
            modifier = Modifier.fillMaxWidth(),
            onClick = onCreate,
        )
        EyespieSecondaryAction(
            text = "Import .eyespie",
            modifier = Modifier.fillMaxWidth(),
            enabled = importEnabled,
            onClick = onImport,
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
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        EyespieSecondaryAction(
            text = actionLabel,
            modifier = Modifier.fillMaxWidth(),
            onClick = onAction,
        )
    }
}
