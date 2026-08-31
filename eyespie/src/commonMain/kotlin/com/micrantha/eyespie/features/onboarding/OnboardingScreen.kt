package com.micrantha.eyespie.features.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.presentation.theme.BrandScene
import com.micrantha.eyespie.presentation.theme.EyespieLogo
import com.micrantha.eyespie.presentation.theme.OnboardingIllustration

private fun OnboardingPage.toScene(): BrandScene = when (this) {
    OnboardingPage.Local -> BrandScene.Local
    OnboardingPage.Create -> BrandScene.Create
    OnboardingPage.Share -> BrandScene.Share
    OnboardingPage.Join -> BrandScene.Join
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    dispatch: (OnboardingIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${state.page.ordinal + 1} of ${OnboardingPage.entries.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EyespieLogo(size = 36.dp)
        }

        OnboardingIllustration(scene = state.page.toScene(), height = 180.dp)

        val eyebrow: String
        val title: String
        val body: String
        when (state.page) {
            OnboardingPage.Local -> {
                eyebrow = "LOCAL MODE"
                title = "Play locally"
                body = "Eyespie works without an account or hosted backend. Games and progress are stored on this device."
            }
            OnboardingPage.Create -> {
                eyebrow = "CREATE A GAME"
                title = "Create a game"
                body = "Capture a target and write a clue locally. Matching uses an on-device image representation instead of uploading the original target image."
            }
            OnboardingPage.Share -> {
                eyebrow = "SHARE A GAME"
                title = "Share a game"
                body = "Export a game as a signed .eyespie file, then share that file with the normal tools on your device. The signature verifies integrity and creator-key continuity; it does not make the bundle secret."
            }
            OnboardingPage.Join -> {
                eyebrow = "JOIN A GAME"
                title = "Join a game"
                body = "Open a .eyespie file shared by another player. Eyespie validates the bundle before adding the playable game to local storage."
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                eyebrow,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.completionFailed) {
            Text(
                "Eyespie couldn't save that onboarding choice. Try again.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.page != OnboardingPage.Local) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !state.completing,
                    onClick = { dispatch(OnboardingIntent.Previous) },
                ) { Text("Back") }
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = !state.completing,
                onClick = { dispatch(OnboardingIntent.Skip) },
            ) { Text(if (state.completing) "Saving…" else "Skip") }
            if (state.page != OnboardingPage.Join) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !state.completing,
                    onClick = { dispatch(OnboardingIntent.Next) },
                ) { Text("Next") }
            } else {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !state.completing,
                    onClick = { dispatch(OnboardingIntent.Done) },
                ) { Text(if (state.completing) "Saving…" else "Start playing") }
            }
        }
    }
}
