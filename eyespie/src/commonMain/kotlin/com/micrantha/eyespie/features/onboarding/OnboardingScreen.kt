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
        when (state.page) {
            OnboardingPage.Welcome -> {
                Text("Welcome, agent", style = MaterialTheme.typography.titleLarge)
                Text("Eyespie works locally without an account or hosted backend. Games and progress stay on this device.")
            }
            OnboardingPage.Create -> {
                Text("Create a mission", style = MaterialTheme.typography.titleLarge)
                Text("Create a clue and target locally. Matching uses a local image representation instead of uploading the original target image.")
            }
            OnboardingPage.Play -> {
                Text("Find the target", style = MaterialTheme.typography.titleLarge)
                Text("Follow the clue, capture a guess, and match locally. Signed .eyespie sharing and import remain separate portable-game flows.")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.page != OnboardingPage.Welcome) {
                OutlinedButton(onClick = { dispatch(OnboardingIntent.Previous) }) { Text("Previous") }
            }
            if (state.page != OnboardingPage.Play) {
                Button(onClick = { dispatch(OnboardingIntent.Next) }) { Text("Next") }
            } else {
                Button(onClick = { dispatch(OnboardingIntent.Done) }) { Text("Done") }
            }
            OutlinedButton(onClick = { dispatch(OnboardingIntent.Back) }) { Text("Back") }
        }
    }
}
