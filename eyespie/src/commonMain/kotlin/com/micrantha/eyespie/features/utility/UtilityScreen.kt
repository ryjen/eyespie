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
import androidx.compose.material3.MaterialTheme
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
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        EyespieTopBar(
            onBack = { dispatch(UtilityIntent.Back) },
            backContentDescription = "Back to field desk",
        )

        EyespieHeader(
            eyebrow = "Local field kit",
            title = "Profile & settings",
            subtitle = "Everything here belongs to this device. Eyespie does not require a hosted account for core play.",
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
                UtilityCard(eyebrow = "Agent identity", title = "Local identity") {
                    Text(content.identityDisplayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Device identity · …${content.identityIdSuffix}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "This identity signs locally authored game files. It is not an online account, login, or cloud profile.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        UtilityCard(eyebrow = "Data boundary", title = "Privacy & sharing") {
            Text(
                "Core Eyespie play is backendless and local-authoritative. Game state and progress are stored on this device.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "The original target photo is used locally to derive a target embedding and is not exported as game authority.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "A signed .eyespie file contains inspectable gameplay data, including target embeddings. Its signature proves integrity and provenance; it does not provide confidentiality, DRM, or anti-cheat secrecy.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        UtilityCard(eyebrow = "Field manual", title = "Help") {
            Text(
                "Create a field case by writing clues and capturing real-world targets. Share the signed .eyespie file, then players use their camera to match each clue on their own device.",
                style = MaterialTheme.typography.bodyMedium,
            )
            EyespieSecondaryAction(
                text = "Show how Eyespie works",
                modifier = Modifier.fillMaxWidth(),
                onClick = { dispatch(UtilityIntent.OnboardingSelected) },
            )
        }

        UtilityCard(eyebrow = "Capture permission", title = "Camera") {
            Text(
                "Camera permission and session lifecycle are owned by the platform camera surface. Eyespie only asks for camera access when capture is needed.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "If camera access is denied, supported capture screens offer the platform Settings recovery action when it is valid. This page does not duplicate system permission controls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun UtilityCard(
    eyebrow: String,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    EyespiePanel {
        EyespieEyebrow(eyebrow)
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        content()
    }
}
