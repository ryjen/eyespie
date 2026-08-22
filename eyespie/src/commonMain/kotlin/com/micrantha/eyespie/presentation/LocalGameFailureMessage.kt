package com.micrantha.eyespie.presentation

import com.micrantha.eyespie.clue.ClueValidationError
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameFailureCode

fun localGameFailureMessage(failure: LocalGameFailure): String = when (failure.code) {
    LocalGameFailureCode.OPERATION_IN_PROGRESS -> "Another local capture or match operation is already running."
    LocalGameFailureCode.INVALID_GAME_NAME -> "Enter a non-empty game name up to 80 characters."
    LocalGameFailureCode.INVALID_CLUE -> when (failure.clueValidationError) {
        ClueValidationError.BLANK_CLUE -> "Enter a clue before capturing the target."
        ClueValidationError.CLUE_TOO_LONG -> "The clue is too long."
        ClueValidationError.BLANK_EXPECTED_ANSWER -> "Enter the creator-only expected answer."
        ClueValidationError.EXPECTED_ANSWER_TOO_LONG -> "The expected answer is too long."
        null -> "The clue authority is invalid."
    }
    LocalGameFailureCode.IDENTITY_UNAVAILABLE -> "The device-local player identity is unavailable."
    LocalGameFailureCode.TARGET_EMBEDDING_FAILED -> "The target image could not be embedded on this device."
    LocalGameFailureCode.GUESS_EMBEDDING_FAILED -> "The guess image could not be embedded on this device."
    LocalGameFailureCode.GAME_NOT_FOUND -> "The selected local game no longer exists."
    LocalGameFailureCode.THING_NOT_FOUND -> "The selected target no longer exists."
    LocalGameFailureCode.NOT_LOCAL_CREATOR -> "Only the local creator can add or change clues for this game."
    LocalGameFailureCode.MATCH_POLICY_INVALID -> "The saved match policy is incompatible with this build."
    LocalGameFailureCode.PERSISTENCE_FAILED -> "Local game state could not be saved or loaded."
}
