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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAction
import com.micrantha.eyespie.features.onboarding.entities.CapabilityAuthorization
import com.micrantha.eyespie.features.onboarding.entities.CapabilityUiState
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction
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

        if (state.capabilities.isEmpty()) {
            Text(
                text = "Checking permissions available on this device…",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            state.capabilities.forEach { capability ->
                PermissionCapabilityCard(
                    capability = capability,
                    requestInFlight = state.requestInFlight,
                    dispatch = dispatch,
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.Padding.large))
        Text(
            text = "Unresolved permissions remain optional and can be configured later.",
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(
            enabled = state.requestInFlight == null,
            onClick = { dispatch(OnboardingAction.NextPage) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun PermissionCapabilityCard(
    capability: CapabilityUiState,
    requestInFlight: com.micrantha.eyespie.features.onboarding.entities.OnboardingCapability?,
    dispatch: Dispatch,
) {
    val status = capability.authorization.label()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = status },
    ) {
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
                text = status,
                style = MaterialTheme.typography.labelLarge,
            )

            val action = capability.action
            val actionLabel = capability.actionLabel
            if (action != null && actionLabel != null) {
                OutlinedButton(
                    enabled = requestInFlight == null,
                    onClick = {
                        when (action) {
                            CapabilityAction.Request -> dispatch(
                                OnboardingAction.RequestCapability(capability.capability)
                            )

                            CapabilityAction.OpenSettings -> dispatch(
                                OnboardingAction.OpenCapabilitySettings(capability.capability)
                            )
                        }
                    },
                ) {
                    Text(
                        if (requestInFlight == capability.capability) {
                            "Requesting…"
                        } else {
                            actionLabel
                        }
                    )
                }
            } else if (capability.capability == com.micrantha.eyespie.features.onboarding.entities.OnboardingCapability.Notifications) {
                Text(
                    text = "Eyespie will ask only when a notification-backed feature provides immediate value.",
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
    CapabilityAuthorization.Denied -> "Not allowed; you may try again"
    CapabilityAuthorization.Restricted -> "Restricted by the system"
    CapabilityAuthorization.SettingsRequired -> "Enable in system settings"
}
