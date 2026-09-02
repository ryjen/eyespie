package com.micrantha.eyespie.features.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.imaging.CameraAvailability
import com.micrantha.eyespie.presentation.CameraLayout
import com.micrantha.eyespie.presentation.cameraUnavailableMessage
import com.micrantha.eyespie.presentation.localGameFailureMessage
import com.micrantha.eyespie.presentation.playCameraPermissionMessage
import com.micrantha.eyespie.presentation.theme.EyespieEyebrow
import com.micrantha.eyespie.presentation.theme.EyespiePanel
import com.micrantha.eyespie.presentation.theme.EyespiePrimaryAction
import com.micrantha.eyespie.presentation.theme.EyespieSecondaryAction
import com.micrantha.eyespie.presentation.theme.EyespieStatusBadge
import com.micrantha.eyespie.presentation.theme.EyespieTopBar
import com.micrantha.eyespie.presentation.theme.extendedColors

@Composable
fun PlayGameScreen(
    state: PlayGameState,
    dispatch: (PlayGameIntent) -> Unit,
) {
    if (state.loading && state.content == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
        return
    }

    val content = state.content
    if (content == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EyespieTopBar(onBack = { dispatch(PlayGameIntent.Back) })
            EyespiePanel {
                EyespieEyebrow("Field case")
                Text("Case unavailable", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("This local game or clue is no longer available on this device.")
            }
        }
        return
    }

    CameraLayout(
        onBack = { dispatch(PlayGameIntent.Back) },
        onCaptured = { dispatch(PlayGameIntent.GuessCaptured(it)) },
        onCameraError = { dispatch(PlayGameIntent.CameraFailed) },
        onAvailabilityChanged = { availability ->
            if (availability == CameraAvailability.Unavailable) {
                dispatch(PlayGameIntent.CameraFailed)
            }
        },
        busy = state.busy,
        recoveryMessage = playCameraPermissionMessage(),
        captureButton = { capture ->
            if (!state.completed && !state.matched) {
                EyespiePrimaryAction(
                    text = if (state.busy) "Checking…" else "Check this object",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = capture,
                    enabled = !state.busy,
                )
            }
        },
    ) {
        state.failure?.let { failure ->
            EyespiePanel(containerColor = MaterialTheme.colorScheme.errorContainer) {
                EyespieEyebrow("Capture problem", color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    when (failure) {
                        PlayGameFailure.CameraUnavailable -> cameraUnavailableMessage()
                        is PlayGameFailure.Game -> localGameFailureMessage(failure.failure)
                    },
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                EyespieSecondaryAction(
                    text = "Dismiss",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { dispatch(PlayGameIntent.DismissFailure) },
                )
            }
        }

        EyespiePanel(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EyespieEyebrow("Field case")
                EyespieStatusBadge("${state.matchedClues} / ${content.clueCount} found")
            }
            Text(content.gameName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Clue ${content.clueNumber} of ${content.clueCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val progress = if (content.clueCount == 0) 0f else state.matchedClues.toFloat() / content.clueCount
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer,
            )
        }

        EyespiePanel(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ) {
            EyespieEyebrow("Your clue")
            Text(content.clueText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        when {
            state.completed -> CompletionCard(onBack = { dispatch(PlayGameIntent.Back) })
            state.matched -> FoundCard(
                hasNext = content.nextThingId != null,
                onNext = { dispatch(PlayGameIntent.NextClueSelected) },
                onBack = { dispatch(PlayGameIntent.Back) },
            )
            else -> {
                state.latestOutcome?.let { outcome ->
                    if (!outcome.match.matched) {
                        EyespiePanel(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.94f),
                        ) {
                            EyespieEyebrow("Keep searching", color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text("Not it yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Try another angle or move closer to the object described by the clue.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoundCard(
    hasNext: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = extendedColors
    EyespiePanel(
        containerColor = colors.successContainer,
        contentColor = colors.onSuccessContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(30.dp))
            Column {
                EyespieEyebrow("Match confirmed", color = colors.success)
                Text("Clue found", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        Text("The match has been saved to this device.")
        if (hasNext) {
            EyespiePrimaryAction(
                text = "Next clue",
                modifier = Modifier.fillMaxWidth(),
                onClick = onNext,
            )
        } else {
            EyespieSecondaryAction(
                text = "Back to game",
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack,
            )
        }
    }
}

@Composable
private fun CompletionCard(onBack: () -> Unit) {
    val colors = extendedColors
    EyespiePanel(
        containerColor = colors.successContainer,
        contentColor = colors.onSuccessContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(30.dp))
            Column {
                EyespieEyebrow("Mission resolved", color = colors.success)
                Text("Case complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        Text("Every clue in this game has been found. Progress is stored on this device.")
        EyespiePrimaryAction(
            text = "Back to game",
            modifier = Modifier.fillMaxWidth(),
            onClick = onBack,
        )
    }
}
