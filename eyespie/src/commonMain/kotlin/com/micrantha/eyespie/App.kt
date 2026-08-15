package com.micrantha.eyespie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.clue.ClueValidationError
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameFailureCode
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.game.LocalGameSummary
import com.micrantha.eyespie.game.PlayableThingSummary
import com.micrantha.eyespie.imaging.CameraCapture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private sealed interface AppScreen {
    data object Home : AppScreen
    data object Create : AppScreen
    data class Play(val gameId: GameId, val thingId: ThingId) : AppScreen
}

private sealed interface UiFailure {
    data class Game(val failure: LocalGameFailure) : UiFailure
    data object CameraUnavailable : UiFailure
}

@Composable
fun App(runtime: EyespieRuntime) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val scope = rememberCoroutineScope()
            var snapshot by remember(runtime) { mutableStateOf<LocalGameSnapshot?>(null) }
            var loading by remember(runtime) { mutableStateOf(true) }
            var failure by remember(runtime) { mutableStateOf<UiFailure?>(null) }
            var screen by remember(runtime) { mutableStateOf<AppScreen>(AppScreen.Home) }

            suspend fun refreshSnapshot() {
                loading = true
                when (val result = runtime.gameLoop.loadSnapshot()) {
                    is LocalGameResult.Success -> {
                        snapshot = result.value
                        failure = null
                    }
                    is LocalGameResult.Failure -> failure = UiFailure.Game(result.failure)
                }
                loading = false
            }

            LaunchedEffect(runtime) {
                refreshSnapshot()
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Eyespie", style = MaterialTheme.typography.headlineLarge)
                Text("Offline travel-spy game", style = MaterialTheme.typography.titleMedium)

                failure?.let {
                    FailureBanner(
                        failure = it,
                        onDismiss = { failure = null },
                    )
                }

                Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when {
                        loading && snapshot == null -> {
                            Spacer(Modifier.height(24.dp))
                            CircularProgressIndicator()
                        }

                        screen == AppScreen.Home -> HomeScreen(
                            snapshot = snapshot,
                            onCreate = {
                                failure = null
                                screen = AppScreen.Create
                            },
                            onPlay = { gameId, thingId ->
                                failure = null
                                screen = AppScreen.Play(gameId, thingId)
                            },
                            onRefresh = {
                                scope.launch { refreshSnapshot() }
                            },
                        )

                        screen == AppScreen.Create -> CreateGameScreen(
                            runtime = runtime,
                            onBack = {
                                failure = null
                                screen = AppScreen.Home
                            },
                            onCreated = { _: CreatedGame ->
                                scope.launch {
                                    refreshSnapshot()
                                    screen = AppScreen.Home
                                }
                            },
                            onFailure = { failure = it },
                        )

                        screen is AppScreen.Play -> {
                            val selected = screen as AppScreen.Play
                            val game = snapshot?.games?.firstOrNull { it.id == selected.gameId }
                            val thing = game?.things?.firstOrNull { it.id == selected.thingId }
                            if (game == null || thing == null) {
                                Text("This local game is no longer available.")
                                Button(onClick = { screen = AppScreen.Home }) { Text("Back") }
                            } else {
                                PlayGameScreen(
                                    runtime = runtime,
                                    game = game,
                                    thing = thing,
                                    onBack = {
                                        failure = null
                                        screen = AppScreen.Home
                                    },
                                    onGuessed = { _: GuessOutcome ->
                                        scope.launch { refreshSnapshot() }
                                    },
                                    onFailure = { failure = it },
                                )
                            }
                        }
                    }
                }
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
                    "Verify the packaged image-embedding model and relaunch the app.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    snapshot: LocalGameSnapshot?,
    onCreate: () -> Unit,
    onPlay: (GameId, ThingId) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        snapshot?.let {
            Text(
                "Local agent: ${it.identity.displayName}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "Identity ${it.identity.id.value.takeLast(12)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCreate) { Text("Create game") }
            OutlinedButton(onClick = onRefresh) { Text("Refresh") }
        }

        HorizontalDivider()
        Text("Local games", style = MaterialTheme.typography.titleLarge)

        val games = snapshot?.games.orEmpty()
        if (games.isEmpty()) {
            Text("No games yet. Create a target and clue entirely on this device.")
        }
        games.forEach { game ->
            Text(game.name, style = MaterialTheme.typography.titleMedium)
            if (game.things.isEmpty()) {
                Text("No playable targets in this game.")
            }
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
                OutlinedButton(onClick = { onPlay(game.id, thing.id) }) {
                    Text("Play")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CreateGameScreen(
    runtime: EyespieRuntime,
    onBack: () -> Unit,
    onCreated: (CreatedGame) -> Unit,
    onFailure: (UiFailure) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var clue by remember { mutableStateOf(TextFieldValue("")) }
    var answer by remember { mutableStateOf(TextFieldValue("")) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Create local game", style = MaterialTheme.typography.titleLarge)
        Text("The target image is used to derive an embedding; it is not saved as game authority.")

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Game name") },
            enabled = !busy,
            singleLine = true,
        )
        OutlinedTextField(
            value = clue,
            onValueChange = { clue = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Clue") },
            enabled = !busy,
        )
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Expected answer (creator-only)") },
            enabled = !busy,
            singleLine = true,
        )

        CameraCapture(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            onCameraError = { onFailure(UiFailure.CameraUnavailable) },
            onCaptured = { targetImage ->
                if (!busy) {
                    scope.launch {
                        busy = true
                        try {
                            when (
                                val result = runtime.gameLoop.createGame(
                                    name = name.text,
                                    clueText = clue.text,
                                    expectedAnswer = answer.text,
                                    targetImage = targetImage,
                                )
                            ) {
                                is LocalGameResult.Success -> onCreated(result.value)
                                is LocalGameResult.Failure -> onFailure(UiFailure.Game(result.failure))
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } finally {
                            busy = false
                        }
                    }
                }
            },
            captureButton = { capture ->
                Button(
                    onClick = capture,
                    enabled = !busy && name.text.isNotBlank() && clue.text.isNotBlank() && answer.text.isNotBlank(),
                ) {
                    Text(if (busy) "Creating…" else "Capture target & create")
                }
            },
        )

        OutlinedButton(onClick = onBack, enabled = !busy) { Text("Back") }
    }
}

@Composable
private fun PlayGameScreen(
    runtime: EyespieRuntime,
    game: LocalGameSummary,
    thing: PlayableThingSummary,
    onBack: () -> Unit,
    onGuessed: (GuessOutcome) -> Unit,
    onFailure: (UiFailure) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var busy by remember(game.id, thing.id) { mutableStateOf(false) }
    var latestOutcome by remember(game.id, thing.id) { mutableStateOf<GuessOutcome?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(game.name, style = MaterialTheme.typography.titleLarge)
        Text("Clue", style = MaterialTheme.typography.titleSmall)
        Text(thing.clue.clueText, style = MaterialTheme.typography.headlineSmall)

        val progress = latestOutcome?.progress ?: thing.progress
        progress?.let {
            Text(
                if (it.matched) {
                    "Progress: matched · best ${formatSimilarity(it.bestSimilarity)}"
                } else {
                    "Progress: best ${formatSimilarity(it.bestSimilarity)}"
                },
            )
        }

        latestOutcome?.let {
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
            onCameraError = { onFailure(UiFailure.CameraUnavailable) },
            onCaptured = { guessImage ->
                if (!busy) {
                    scope.launch {
                        busy = true
                        try {
                            when (
                                val result = runtime.gameLoop.guess(
                                    gameId = game.id,
                                    thingId = thing.id,
                                    guessImage = guessImage,
                                )
                            ) {
                                is LocalGameResult.Success -> {
                                    latestOutcome = result.value
                                    onGuessed(result.value)
                                }
                                is LocalGameResult.Failure -> onFailure(UiFailure.Game(result.failure))
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } finally {
                            busy = false
                        }
                    }
                }
            },
            captureButton = { capture ->
                Button(onClick = capture, enabled = !busy) {
                    Text(if (busy) "Matching…" else "Capture guess")
                }
            },
        )

        OutlinedButton(onClick = onBack, enabled = !busy) { Text("Back") }
    }
}

@Composable
private fun FailureBanner(
    failure: UiFailure,
    onDismiss: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
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
            OutlinedButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

private fun failureMessage(failure: UiFailure): String = when (failure) {
    UiFailure.CameraUnavailable -> "Camera access is unavailable. Check permission/settings and try again."
    is UiFailure.Game -> when (failure.failure.code) {
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

private fun clueFailureMessage(error: ClueValidationError?): String = when (error) {
    ClueValidationError.BLANK_CLUE -> "Enter a clue before capturing the target."
    ClueValidationError.CLUE_TOO_LONG -> "The clue is too long."
    ClueValidationError.BLANK_EXPECTED_ANSWER -> "Enter the creator-only expected answer."
    ClueValidationError.EXPECTED_ANSWER_TOO_LONG -> "The expected answer is too long."
    null -> "The clue authority is invalid."
}

private fun formatSimilarity(value: Double?): String {
    if (value == null) return "—"
    val percentageTenths = (value * 1000.0).toInt()
    return "${percentageTenths / 10.0}%"
}
