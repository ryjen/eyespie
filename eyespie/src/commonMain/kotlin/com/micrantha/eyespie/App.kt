package com.micrantha.eyespie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.clue.ClueValidationError
import com.micrantha.eyespie.game.ManualGameDraft
import com.micrantha.eyespie.game.OfflineFailure
import com.micrantha.eyespie.game.OfflineFailureCode
import com.micrantha.eyespie.game.OfflineGameCoordinator
import com.micrantha.eyespie.game.OfflineResult
import com.micrantha.eyespie.game.OfflineRuntimeState
import com.micrantha.eyespie.game.PlayableGameState
import com.micrantha.eyespie.imaging.CameraCapture
import kotlinx.coroutines.launch

private enum class AppMode {
    CREATE,
    PLAY,
}

private sealed interface UiFailure {
    data object Camera : UiFailure
    data class Offline(val failure: OfflineFailure) : UiFailure
}

@Composable
fun App(runtime: OfflineRuntimeState) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (runtime) {
                is OfflineRuntimeState.Ready -> OfflineGameScreen(runtime.coordinator)
                is OfflineRuntimeState.Unavailable -> RuntimeUnavailableScreen()
            }
        }
    }
}

@Composable
private fun RuntimeUnavailableScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Eyespie", style = MaterialTheme.typography.headlineLarge)
        Text("Local storage is unavailable. Restart the app and try again.")
    }
}

@Composable
private fun OfflineGameScreen(coordinator: OfflineGameCoordinator) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(AppMode.CREATE) }
    var games by remember { mutableStateOf<List<PlayableGameState>>(emptyList()) }
    var selectedGameId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<UiFailure?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(coordinator) {
        busy = true
        try {
            when (val loaded = coordinator.loadGames()) {
                is OfflineResult.Success -> {
                    games = loaded.value
                    if (games.isNotEmpty()) {
                        selectedGameId = games.first().id.value
                        mode = AppMode.PLAY
                    }
                }
                is OfflineResult.Failure -> failure = UiFailure.Offline(loaded.failure)
            }
        } finally {
            busy = false
        }
    }

    val selectedGame = games.firstOrNull { it.id.value == selectedGameId } ?: games.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Eyespie", style = MaterialTheme.typography.headlineLarge)
        Text("Offline travel spy game", style = MaterialTheme.typography.titleMedium)

        failure?.let {
            Text(
                text = failureMessage(it),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

        if (games.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Local games", style = MaterialTheme.typography.titleSmall)
                games.forEach { game ->
                    TextButton(
                        onClick = {
                            selectedGameId = game.id.value
                            mode = AppMode.PLAY
                            failure = null
                            status = null
                        },
                        enabled = !busy,
                    ) {
                        Text(game.name)
                    }
                }
            }
        }

        when (mode) {
            AppMode.CREATE -> CreateGameScreen(
                coordinator = coordinator,
                busy = busy,
                onBusy = { busy = it },
                onFailure = {
                    failure = it
                    status = null
                },
                onCreated = { game ->
                    games = listOf(game) + games.filterNot { it.id == game.id }
                    selectedGameId = game.id.value
                    failure = null
                    status = "Challenge saved locally."
                    mode = AppMode.PLAY
                },
            )
            AppMode.PLAY -> if (selectedGame == null) {
                Text("No local challenge is available yet.")
            } else {
                PlayGameScreen(
                    coordinator = coordinator,
                    game = selectedGame,
                    busy = busy,
                    onBusy = { busy = it },
                    onFailure = {
                        failure = it
                        status = null
                    },
                    onUpdated = { updated, matchStatus ->
                        games = games.map { if (it.id == updated.id) updated else it }
                        failure = null
                        status = matchStatus
                    },
                )
            }
        }

        TextButton(
            onClick = {
                mode = if (mode == AppMode.CREATE) AppMode.PLAY else AppMode.CREATE
                failure = null
                status = null
            },
            enabled = !busy && (mode == AppMode.PLAY || games.isNotEmpty()),
        ) {
            Text(if (mode == AppMode.CREATE) "Back to game" else "Create a new game")
        }
    }
}

@Composable
private fun CreateGameScreen(
    coordinator: OfflineGameCoordinator,
    busy: Boolean,
    onBusy: (Boolean) -> Unit,
    onFailure: (UiFailure) -> Unit,
    onCreated: (PlayableGameState) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var gameName by remember { mutableStateOf("") }
    var clueText by remember { mutableStateOf("") }
    var expectedAnswer by remember { mutableStateOf("") }
    var pendingDraft by remember { mutableStateOf<ManualGameDraft?>(null) }

    Text("Create challenge", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = gameName,
        onValueChange = { gameName = it },
        label = { Text("Game name") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy,
        singleLine = true,
    )
    OutlinedTextField(
        value = clueText,
        onValueChange = { clueText = it },
        label = { Text("Clue") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy,
    )
    OutlinedTextField(
        value = expectedAnswer,
        onValueChange = { expectedAnswer = it },
        label = { Text("Expected answer (creator only)") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy,
        singleLine = true,
    )

    CameraCapture(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        onCameraError = {
            pendingDraft = null
            onBusy(false)
            onFailure(UiFailure.Camera)
        },
        onCaptured = { image ->
            val draft = pendingDraft
            if (draft == null) {
                onFailure(UiFailure.Camera)
            } else {
                onBusy(true)
                scope.launch {
                    try {
                        when (val created = coordinator.createManualGame(draft, image)) {
                            is OfflineResult.Success -> {
                                expectedAnswer = ""
                                clueText = ""
                                gameName = ""
                                pendingDraft = null
                                onCreated(created.value)
                            }
                            is OfflineResult.Failure -> {
                                pendingDraft = null
                                onFailure(UiFailure.Offline(created.failure))
                            }
                        }
                    } finally {
                        onBusy(false)
                    }
                }
            }
        },
        captureButton = { capture ->
            Button(
                onClick = {
                    val draft = ManualGameDraft(gameName, clueText, expectedAnswer)
                    val invalid = coordinator.validateDraft(draft)
                    if (invalid != null) {
                        onFailure(UiFailure.Offline(invalid))
                    } else {
                        pendingDraft = draft
                        capture()
                    }
                },
                enabled = !busy,
            ) {
                Text("Capture target")
            }
        },
    )
}

@Composable
private fun PlayGameScreen(
    coordinator: OfflineGameCoordinator,
    game: PlayableGameState,
    busy: Boolean,
    onBusy: (Boolean) -> Unit,
    onFailure: (UiFailure) -> Unit,
    onUpdated: (PlayableGameState, String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val thing = game.things.firstOrNull()

    Text(game.name, style = MaterialTheme.typography.titleLarge)
    if (thing == null) {
        Text("This local game has no challenge to play.")
        return
    }

    Text("Clue", style = MaterialTheme.typography.titleSmall)
    Text(thing.clue.clueText, style = MaterialTheme.typography.bodyLarge)
    thing.bestSimilarity?.let { best ->
        Text("Best match: ${(best * 100).toInt()}%")
    }
    if (thing.matched) {
        Text("Matched")
    }

    CameraCapture(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        onCameraError = {
            onBusy(false)
            onFailure(UiFailure.Camera)
        },
        onCaptured = { image ->
            onBusy(true)
            scope.launch {
                try {
                    when (val guessed = coordinator.guess(game.id, thing.id, image)) {
                        is OfflineResult.Success -> {
                            val percent = (guessed.value.similarity * 100).toInt()
                            val message = if (guessed.value.matched) {
                                "Matched at $percent%."
                            } else {
                                "Not a match: $percent%. Try again."
                            }
                            onUpdated(guessed.value.game, message)
                        }
                        is OfflineResult.Failure -> onFailure(UiFailure.Offline(guessed.failure))
                    }
                } finally {
                    onBusy(false)
                }
            }
        },
        captureButton = { capture ->
            Button(onClick = capture, enabled = !busy) {
                Text("Capture guess")
            }
        },
    )
}

private fun failureMessage(failure: UiFailure): String = when (failure) {
    UiFailure.Camera -> "Camera capture is unavailable. Check permission and try again."
    is UiFailure.Offline -> when (failure.failure.code) {
        OfflineFailureCode.BUSY -> "Another capture is still being processed."
        OfflineFailureCode.INVALID_GAME_NAME -> "Enter a game name between 1 and 120 characters."
        OfflineFailureCode.INVALID_CLUE -> clueFailureMessage(failure.failure.clueValidationError)
        OfflineFailureCode.IDENTITY_UNAVAILABLE -> "Local player identity is unavailable."
        OfflineFailureCode.EMBEDDING_FAILED -> "On-device image analysis failed. Try another capture."
        OfflineFailureCode.INVALID_EMBEDDING -> "The image result was invalid and was not saved."
        OfflineFailureCode.GAME_NOT_FOUND -> "That local game is no longer available."
        OfflineFailureCode.THING_NOT_FOUND -> "That challenge is no longer available."
        OfflineFailureCode.PERSISTENCE_FAILED -> "Local storage failed. No partial challenge was accepted."
        OfflineFailureCode.ID_GENERATION_FAILED -> "A local identifier could not be created. Try again."
    }
}

private fun clueFailureMessage(error: ClueValidationError?): String = when (error) {
    ClueValidationError.BLANK_CLUE -> "Enter a clue."
    ClueValidationError.CLUE_TOO_LONG -> "The clue is too long."
    ClueValidationError.BLANK_EXPECTED_ANSWER -> "Enter the creator-only expected answer."
    ClueValidationError.EXPECTED_ANSWER_TOO_LONG -> "The expected answer is too long."
    null -> "The clue is invalid."
}
