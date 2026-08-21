package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.clue.ClueValidationError
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.imaging.CapturedImage

sealed interface AppScreen {
    data object Home : AppScreen
    data object Create : AppScreen
    data class Play(val gameId: GameId, val thingId: ThingId) : AppScreen
}

sealed interface AppFailure {
    data class Game(val failure: LocalGameFailure) : AppFailure
    data object CameraUnavailable : AppFailure
}

data class CreateGameFormState(
    val name: String = "",
    val clue: String = "",
    val expectedAnswer: String = "",
)

data class AppState(
    val snapshot: LocalGameSnapshot? = null,
    val screen: AppScreen = AppScreen.Home,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val failure: AppFailure? = null,
    val createForm: CreateGameFormState = CreateGameFormState(),
    val latestOutcome: GuessOutcome? = null,
)

sealed interface AppIntent {
    data object Refresh : AppIntent
    data object NavigateHome : AppIntent
    data object NavigateCreate : AppIntent
    data class NavigatePlay(val gameId: GameId, val thingId: ThingId) : AppIntent
    data object DismissFailure : AppIntent
    data object CameraFailed : AppIntent

    data class CreateNameChanged(val value: String) : AppIntent
    data class CreateClueChanged(val value: String) : AppIntent
    data class CreateExpectedAnswerChanged(val value: String) : AppIntent
    data class CreateTargetCaptured(val image: CapturedImage) : AppIntent
    data class GuessCaptured(val image: CapturedImage) : AppIntent

    data class SnapshotLoaded(val snapshot: LocalGameSnapshot) : AppIntent
    data class OperationFailed(val failure: LocalGameFailure) : AppIntent
    data class GameCreated(val snapshot: LocalGameSnapshot) : AppIntent
    data class GuessCompleted(val outcome: GuessOutcome, val snapshot: LocalGameSnapshot?) : AppIntent
}

internal fun clueFailureMessage(error: ClueValidationError?): String = when (error) {
    ClueValidationError.BLANK_CLUE -> "Enter a clue before capturing the target."
    ClueValidationError.CLUE_TOO_LONG -> "The clue is too long."
    ClueValidationError.BLANK_EXPECTED_ANSWER -> "Enter the creator-only expected answer."
    ClueValidationError.EXPECTED_ANSWER_TOO_LONG -> "The expected answer is too long."
    null -> "The clue authority is invalid."
}
