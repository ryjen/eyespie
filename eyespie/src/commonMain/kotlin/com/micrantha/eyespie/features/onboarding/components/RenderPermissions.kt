package com.micrantha.eyespie.features.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.onboarding.entities.CapabilityUiState
import com.micrantha.eyespie.features.onboarding.entities.OnboardingUiState

@Composable
fun BoxScope.RenderPermissions(state: OnboardingUiState, dispatch: Dispatch) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimensions.Padding.medium),
        modifier = Modifier
            .padding(Dimensions.screen)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Permissions and privacy",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = "Choose permissions individually. You can continue without granting them and enable them later when a feature needs access.",
            style = MaterialTheme.typography.bodyLarge,
        )

        state.capabilities.forEach { capability ->
            PermissionCapabilityCard(capability)
        }

        Spacer(modifier = Modifier.height(Dimensions.Padding.large))
        Text(
            text = "Unresolved permissions remain optional and can be configured later.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PermissionCapabilityCard(capability: CapabilityUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimensions.Padding.small),
            modifier = Modifier.padding(Dimensions.Padding.large),
        ) {
            Text(
                text = capability.title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = capability.rationale,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = capability.deniedImpact,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = capability.privacySummary,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = capability.authorization.label(),
                style = MaterialTheme.typography.labelLarge,
            )
            if (capability.canRequestDuringOnboarding) {
                OutlinedButton(
                    enabled = false,
                    onClick = { },
                ) {
                    Text("Allow")
                }
                Text(
                    text = "Permission requests will be enabled by the platform authorization adapter.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    text = "You will be asked when this feature provides immediate value.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun CapabilityAuthorization.label(): String = when (this) {
    CapabilityAuthorization.Unsupported -> "Unavailable on this platform"
    CapabilityAuthorization.NotRequired -> "No system permission required"
    CapabilityAuthorization.NotRequested -> "Not requested"
    CapabilityAuthorization.Granted -> "Allowed"
    CapabilityAuthorization.Denied -> "Not allowed"
    CapabilityAuthorization.Restricted -> "Restricted by the system"
    CapabilityAuthorization.SettingsRequired -> "Open system settings to enable"
}
