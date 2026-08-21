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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.features.app.AppFailure
import com.micrantha.eyespie.features.app.AppIntent
import com.micrantha.eyespie.features.app.AppInteractor
import com.micrantha.eyespie.features.app.AppScreen
import com.micrantha.eyespie.features.app.AppState
import com.micrantha.eyespie.features.app.clueFailureMessage
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.game.LocalGameFailureCode
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.game.LocalGameSummary
import com.micrantha.eyespie.game.PlayableThingSummary
import com.micrantha.eyespie.imaging.CameraCapture

@Composable
fun App(runtime: EyespieRuntime) {
    val scope = rememberCoroutineScope()
    val interactor = remember(runtime, scope) { AppInteractor(runtime, scope) }
    val state by interactor.state.collectAsState()

    LaunchedEffect(interactor) {
        interactor.dispatch(AppIntent.Refresh)
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppView(state = state, dispatch = interactor::dispatch)
        }
    }
}

@Composable
private fun AppView(
    state: AppState,
    dispatch: (AppIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Eyespie", style = MaterialTheme.typography.headlineLarge)
        Text("Offline travel-spy game", style = MaterialTheme.typography.titleMedium)

        state.failure?.let { FailureBanner(it, dispatch) }

        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                state.loading && state.snapshot == null -> {
                    Spacer(Modifier.height(24.dp))
                    CircularProgressIndicator()
                }
                state.screen == AppScreen.Home -> HomeView(state.snapshot, dispatch)
                state.screen == AppScreen.Create -> CreateGameView(state, dispatch)
                state.screen is AppScreen.Play -> PlayGameView(state, dispatch)
            }
        }
    }
}

@Composable
fun AppUnavailable() {
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
                Text(
                    "Verify required local components and relaunch the app.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun HomeView(
    snapshot: LocalGameSnapshot?,
    dispatch: (AppIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        snapshot?.let {
            Text("Local agent: ${it.identity.displayName}", style = MaterialTheme.typography.titleSmall)
            Text("Identity ${it.identity.id.value.takeLast(12)}", style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { dispatch(AppIntent.NavigateCreate) }) { Text("Create game") }
            OutlinedButton(onClick = { dispatch(AppIntent.Refresh) }) { Text("Refresh") }
        }

        HorizontalDivider()
        Text("Local games", style = MaterialTheme.typography.titleLarge)

        val games = snapshot?.games.orEmpty()
        if (games.isEmpty()) {
            Text("No games yet. Create a target and clue entirely on this device.")
        }
        games.forEach { game ->
            Text(game.name, style = MaterialTheme.typography.titleMedium)
            if (game.things.isEmpty()) Text("No playable targets in this game.")
            game.things.forEach { thing ->
                Text(thing.clue.clueText, style = MaterialTheme.typography.bodyLarge)
                thing.progress?.let { progress ->
                    Text(
                        if (progress.matched) {
                            "Matched · best ${formatSimilarity(progress.bestSimilarity)}"
                        } else {
                            "Best ${formatSimilarity(progress.bestSimilarity)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedButton(onClick = { dispatch(AppIntent.NavigatePlay(game.id, thing.id)) }) {
                    Text("Play")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CreateGameView(
    state: AppState,
    dispatch: (AppIntent) -> Unit,
) {
    val form = state.createForm
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Create local game", style = MaterialTheme.typography.titleLarge)
        Text("The target image is used to derive an embedding; it is not saved as game authority.")

        OutlinedTextField(
            value = form.name,
            onValueChange = { dispatch(AppIntent.CreateNameChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Game name") },
            enabled = !state.busy,
            singleLine = true,
        )
        OutlinedTextField(
            value = form.clue,
            onValueChange = { dispatch(AppIntent.CreateClueChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Clue") },
            enabled = !state.busy,
        )
        OutlinedTextField(
            value = form.expectedAnswer,
            onValueChange = { dispatch(AppIntent.CreateExpectedAnswerChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Expected answer (creator-only)") },
            enabled = !state.busy,
            singleLine = true,
        )

        CameraCapture(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            onCameraError = { dispatch(AppIntent.CameraFailed) },
            onCaptured = { dispatch(AppIntent.CreateTargetCaptured(it)) },
            captureButton = { capture ->
                Button(
                    onClick = capture,
                    enabled = !state.busy &&
                        form.name.isNotBlank() &&
                        form.clue.isNotBlank() &&
                        form.expectedAnswer.isNotBlank(),
                ) {
                    Text(if (state.busy) "Creating…" else "Capture target & create")
                }
            },
        )

        OutlinedButton(
            onClick = { dispatch(AppIntent.NavigateHome) },
            enabled = !state.busy,
        ) { Text("Back") }
    }
}

@Composable
private fun PlayGameView(
    state: AppState,
    dispatch: (AppIntent) -> Unit,
) {
    val game: LocalGameSummary? = state.playGame
    val thing: PlayableThingSummary? = state.playThing
    if (game == null || thing == null) {
        Text("This local game is no longer available.")
        Button(onClick = { dispatch(AppIntent.NavigateHome) }) { Text("Back") }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(game.name, style = MaterialTheme.typography.titleLarge)
        Text("Clue", style = MaterialTheme.typography.titleSmall)
        Text(thing.clue.clueText, style = MaterialTheme.typography.headlineSmall)

        val progress = state.latestOutcome?.progress ?: thing.progress
        progress?.let {
            Text(
                if (it.matched) {
                    "Progress: matched · best ${formatSimilarity(it.bestSimilarity)}"
                } else {
                    "Progress: best ${formatSimilarity(it.bestSimilarity)}"
                },
            )
        }

        state.latestOutcome?.let {
            Text(
                if (it.match.matched) {
                    "Match · similarity ${formatSimilarity(it.match.similarity)}"
                } else {
                    "Not a match · similarity ${formatSimilarity(it.match.similarity)}"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }

        CameraCapture(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            onCameraError = { dispatch(AppIntent.CameraFailed) },
            onCaptured = { dispatch(AppIntent.GuessCaptured(it)) },
            captureButton = { capture ->
                Button(onClick = capture, enabled = !state.busy) {
                    Text(if (state.busy) "Matching…" else "Capture guess")
                }
            },
        )

        OutlinedButton(
            onClick = { dispatch(AppIntent.NavigateHome) },
            enabled = !state.busy,
        ) { Text("Back") }
    }
}

@Composable
private fun FailureBanner(
    failure: AppFailure,
    dispatch: (AppIntent) -> Unit,
) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = failureMessage(failure),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = { dispatch(AppIntent.DismissFailure) }) { Text("Dismiss") }
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
