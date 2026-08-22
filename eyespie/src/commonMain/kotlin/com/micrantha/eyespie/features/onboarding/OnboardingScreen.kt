package com.micrantha.eyespie.features.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    dispatch: (OnboardingIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "${state.page.ordinal + 1} of ${OnboardingPage.entries.size}",
            style = MaterialTheme.typography.labelLarge,
        )

        when (state.page) {
            OnboardingPage.Local -> {
                Text("Play locally", style = MaterialTheme.typography.titleLarge)
                Text("Eyespie works without an account or hosted backend. Games and progress are stored on this device.")
            }
            OnboardingPage.Create -> {
                Text("Create a game", style = MaterialTheme.typography.titleLarge)
                Text("Capture a target and write a clue locally. Matching uses an on-device image representation instead of uploading the original target image.")
            }
            OnboardingPage.Share -> {
                Text("Share a game", style = MaterialTheme.typography.titleLarge)
                Text("Export a game as a signed .eyespie file, then share that file with the normal tools on your device. The signature verifies integrity and creator-key continuity; it does not make the bundle secret.")
            }
            OnboardingPage.Join -> {
                Text("Join a game", style = MaterialTheme.typography.titleLarge)
                Text("Open a .eyespie file shared by another player. Eyespie validates the bundle before adding the playable game to local storage.")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.page != OnboardingPage.Local) {
                OutlinedButton(onClick = { dispatch(OnboardingIntent.Previous) }) { Text("Back") }
            }
            OutlinedButton(onClick = { dispatch(OnboardingIntent.Skip) }) { Text("Skip") }
            if (state.page != OnboardingPage.Join) {
                Button(onClick = { dispatch(OnboardingIntent.Next) }) { Text("Next") }
            } else {
                Button(onClick = { dispatch(OnboardingIntent.Done) }) { Text("Start playing") }
            }
        }
    }
}
