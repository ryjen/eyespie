package com.micrantha.eyespie.features.utility

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.presentation.localGameFailureMessage
import com.micrantha.eyespie.presentation.theme.EyespieEyebrow
import com.micrantha.eyespie.presentation.theme.EyespieHeader
import com.micrantha.eyespie.presentation.theme.EyespiePanel
import com.micrantha.eyespie.presentation.theme.EyespieSecondaryAction
import com.micrantha.eyespie.presentation.theme.EyespieTopBar

@Composable
fun UtilityScreen(
    state: UtilityState,
    dispatch: (UtilityIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        EyespieTopBar(
            onBack = { dispatch(UtilityIntent.Back) },
            backContentDescription = "Back to field desk",
        )

        EyespieHeader(
            eyebrow = "Local field kit",
            title = "Profile & settings",
            subtitle = "Local identity, privacy, sharing and capture guidance for this device.",
        )

        state.failure?.let { failure ->
            EyespiePanel(containerColor = MaterialTheme.colorScheme.errorContainer) {
                EyespieEyebrow("Local data unavailable", color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    localGameFailureMessage(failure),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                EyespieSecondaryAction(
                    text = "Retry",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { dispatch(UtilityIntent.Retry) },
                )
            }
        }

        if (state.loading && state.content == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            state.content?.let { content ->
                UtilitySection(eyebrow = "Agent identity", title = "Local identity") {
                    Text(content.identityDisplayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Device identity · …${content.identityIdSuffix}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Signs locally authored game files. This is not an online account, login, or cloud profile.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        UtilitySection(eyebrow = "Data boundary", title = "Privacy & sharing") {
            Text(
                "Core play is backendless and local-authoritative. Game state and progress stay on this device.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Target photos are processed locally into embeddings. Signed .eyespie files contain inspectable gameplay data; signatures prove integrity and provenance, not secrecy.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        UtilitySection(eyebrow = "Field manual", title = "Help") {
            Text(
                "Create clues around real-world targets, share the signed .eyespie file, then players match each clue with their camera.",
                style = MaterialTheme.typography.bodyMedium,
            )
            EyespieSecondaryAction(
                text = "Show how Eyespie works",
                modifier = Modifier.fillMaxWidth(),
                onClick = { dispatch(UtilityIntent.OnboardingSelected) },
            )
        }

        UtilitySection(eyebrow = "Capture permission", title = "Camera", showDivider = false) {
            Text(
                "Camera access is requested only on capture screens and remains owned by the platform camera surface.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "When permission is denied, supported capture screens offer the platform Settings recovery action. This page does not duplicate system permission controls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun UtilitySection(
    eyebrow: String,
    title: String,
    showDivider: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EyespieEyebrow(eyebrow)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                )
            }
        }
    }
}
