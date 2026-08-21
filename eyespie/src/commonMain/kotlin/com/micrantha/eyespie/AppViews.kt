package com.micrantha.eyespie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.features.app.AppFailure
import com.micrantha.eyespie.features.app.AppScreen
import com.micrantha.eyespie.features.app.clueFailureMessage
import com.micrantha.eyespie.features.create.CreateGameIntent
import com.micrantha.eyespie.features.create.CreateGameState
import com.micrantha.eyespie.features.home.HomeIntent
import com.micrantha.eyespie.features.home.HomeState
import com.micrantha.eyespie.features.onboarding.OnboardingIntent
import com.micrantha.eyespie.features.onboarding.OnboardingPage
import com.micrantha.eyespie.features.onboarding.OnboardingState
import com.micrantha.eyespie.features.play.PlayGameIntent
import com.micrantha.eyespie.features.play.PlayGameState
import com.micrantha.eyespie.game.LocalGameFailureCode
import com.micrantha.eyespie.imaging.CameraCapture

@Composable
internal fun HomeView(state: HomeState, dispatch: (HomeIntent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.failure?.let { FailureBanner(it) { dispatch(HomeIntent.DismissFailure) } }
        if (state.loading && state.snapshot == null) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
            return@Column
        }
        state.snapshot?.let {
            Text("Local agent: ${it.identity.displayName}", style = MaterialTheme.typography.titleSmall)
            Text("Identity ${it.identity.id.value.takeLast(12)}", style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { dispatch(HomeIntent.CreateSelected) }) { Text("Create game") }
            OutlinedButton(onClick = { dispatch(HomeIntent.OnboardingSelected) }) { Text("How to play") }
            OutlinedButton(onClick = { dispatch(HomeIntent.Refresh) }) { Text("Refresh") }
        }
        HorizontalDivider()
        Text("Local games", style = MaterialTheme.typography.titleLarge)
        val games = state.snapshot?.games.orEmpty()
        if (games.isEmpty()) Text("No games yet. Create a target and clue entirely on this device.")
        games.forEach { game ->
            Text(game.name, style = MaterialTheme.typography.titleMedium)
            if (game.things.isEmpty()) Text("No playable targets in this game.")
            game.things.forEach { thing ->
                Text(thing.clue.clueText, style = MaterialTheme.typography.bodyLarge)
                thing.progress?.let { progress ->
                    Text(
                        if (progress.matched) "Matched · best ${formatSimilarity(progress.bestSimilarity)}"
                        else "Best ${formatSimilarity(progress.bestSimilarity)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedButton(
                    onClick = { dispatch(HomeIntent.PlaySelected(AppScreen.Play(game.id, thing.id))) },
                ) { Text("Play") }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
internal fun OnboardingView(state: OnboardingState, dispatch: (OnboardingIntent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (state.page) {
            OnboardingPage.Welcome -> {
                Text("Welcome, agent", style = MaterialTheme.typography.titleLarge)
                Text("Eyespie is an offline travel-spy game. One player creates a visual target and clue; another tries to find and match it with the camera.")
            }
            OnboardingPage.Create -> {
                Text("Create a mission", style = MaterialTheme.typography.titleLarge)
                Text("Choose a game name, write a clue and creator-only expected answer, then capture the target. The image is converted to a local embedding rather than stored as game authority.")
            }
            OnboardingPage.Play -> {
                Text("Find the target", style = MaterialTheme.typography.titleLarge)
                Text("Open a local game, follow the clue, and capture a guess. Matching runs locally against the target embedding and progress stays on this device.")
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

@Composable
internal fun CreateGameView(state: CreateGameState, dispatch: (CreateGameIntent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.failure?.let { FailureBanner(it) { dispatch(CreateGameIntent.DismissFailure) } }
        Text("Create local game", style = MaterialTheme.typography.titleLarge)
        Text("The target image is used to derive an embedding; it is not saved as game authority.")
        OutlinedTextField(
            value = state.name,
            onValueChange = { dispatch(CreateGameIntent.NameChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Game name") },
            enabled = !state.busy,
            singleLine = true,
        )
        OutlinedTextField(
            value = state.clue,
            onValueChange = { dispatch(CreateGameIntent.ClueChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Clue") },
            enabled = !state.busy,
        )
        OutlinedTextField(
            value = state.expectedAnswer,
            onValueChange = { dispatch(CreateGameIntent.ExpectedAnswerChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Expected answer (creator-only)") },
            enabled = !state.busy,
            singleLine = true,
        )
        CameraCapture(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            onCameraError = { dispatch(CreateGameIntent.CameraFailed) },
            onCaptured = { dispatch(CreateGameIntent.TargetCaptured(it)) },
            captureButton = { capture ->
                Button(
                    onClick = capture,
                    enabled = !state.busy && state.name.isNotBlank() && state.clue.isNotBlank() && state.expectedAnswer.isNotBlank(),
                ) { Text(if (state.busy) "Creating…" else "Capture target & create") }
            },
        )
        OutlinedButton(onClick = { dispatch(CreateGameIntent.Back) }, enabled = !state.busy) { Text("Back") }
    }
}

@Composable
internal fun PlayGameView(state: PlayGameState, dispatch: (PlayGameIntent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.failure?.let { FailureBanner(it) { dispatch(PlayGameIntent.DismissFailure) } }
        Text(state.game.name, style = MaterialTheme.typography.titleLarge)
        Text("Clue", style = MaterialTheme.typography.titleSmall)
        Text(state.thing.clue.clueText, style = MaterialTheme.typography.headlineSmall)
        val progress = state.latestOutcome?.progress ?: state.thing.progress
        progress?.let {
            Text(if (it.matched) "Progress: matched · best ${formatSimilarity(it.bestSimilarity)}" else "Progress: best ${formatSimilarity(it.bestSimilarity)}")
        }
        state.latestOutcome?.let {
            Text(
                if (it.match.matched) "Match · similarity ${formatSimilarity(it.match.similarity)}"
                else "Not a match · similarity ${formatSimilarity(it.match.similarity)}",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        CameraCapture(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            onCameraError = { dispatch(PlayGameIntent.CameraFailed) },
            onCaptured = { dispatch(PlayGameIntent.GuessCaptured(it)) },
            captureButton = { capture ->
                Button(onClick = capture, enabled = !state.busy) {
                    Text(if (state.busy) "Matching…" else "Capture guess")
                }
            },
        )
        OutlinedButton(onClick = { dispatch(PlayGameIntent.Back) }, enabled = !state.busy) { Text("Back") }
    }
}

@Composable
internal fun AppUnavailable() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Eyespie", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(12.dp))
                Text("The local game runtime could not be initialized.")
                Text("Verify required local components and relaunch the app.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun FailureBanner(failure: AppFailure, onDismiss: () -> Unit) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = failureMessage(failure), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

private fun failureMessage(failure: AppFailure): String = when (failure) {
    AppFailure.CameraUnavailable -> "Camera access is unavailable. Check permission/settings and try again."
    is AppFailure.Game -> when (failure.failure.code) {
        LocalGameFailureCode.OPERATION_IN_PROGRESS -> "Another local capture or match operation is already running."
        LocalGameFailureCode.INVALID_GAME_NAME -> "Enter a non-empty game name up to 80 characters."
        LocalGameFailureCode.INVALID_CLUE -> clueFailureMessage(failure.failure.clueValidationError)
        LocalGameFailureCode.IDENTITY_UNAVAILABLE -> "The device-local player identity is unavailable."
        LocalGameFailureCode.TARGET_EMBEDDING_FAILED -> "The target image could not be embedded on this device."
        LocalGameFailureCode.GUESS_EMBEDDING_FAILED -> "The guess image could not be embedded on this device."
        LocalGameFailureCode.GAME_NOT_FOUND -> "The selected local game no longer exists."
        LocalGameFailureCode.THING_NOT_FOUND -> "The selected target no longer exists."
        LocalGameFailureCode.MATCH_POLICY_INVALID -> "The saved match policy is incompatible with this build."
        LocalGameFailureCode.PERSISTENCE_FAILED -> "Local game state could not be saved or loaded."
    }
}

private fun formatSimilarity(value: Double?): String {
    if (value == null) return "—"
    val percentageTenths = (value * 1000.0).toInt()
    return "${percentageTenths / 10.0}%"
}
