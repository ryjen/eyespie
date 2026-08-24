package com.micrantha.eyespie.presentation

import androidx.compose.runtime.Composable
import com.micrantha.eyespie.clue.ClueValidationError
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameFailureCode
import com.micrantha.eyespie.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun localGameFailureMessage(failure: LocalGameFailure): String = stringResource(
    when (failure.code) {
        LocalGameFailureCode.OPERATION_IN_PROGRESS -> Res.string.failure_operation_in_progress
        LocalGameFailureCode.INVALID_GAME_NAME -> Res.string.failure_invalid_game_name
        LocalGameFailureCode.INVALID_CLUE -> when (failure.clueValidationError) {
            ClueValidationError.BLANK_CLUE -> Res.string.failure_blank_clue
            ClueValidationError.CLUE_TOO_LONG -> Res.string.failure_clue_too_long
            ClueValidationError.BLANK_EXPECTED_ANSWER -> Res.string.failure_blank_expected_answer
            ClueValidationError.EXPECTED_ANSWER_TOO_LONG -> Res.string.failure_expected_answer_too_long
            null -> Res.string.failure_invalid_clue_authority
        }
        LocalGameFailureCode.IDENTITY_UNAVAILABLE -> Res.string.failure_identity_unavailable
        LocalGameFailureCode.NOT_LOCAL_CREATOR -> Res.string.failure_not_local_creator
        LocalGameFailureCode.TARGET_EMBEDDING_FAILED -> Res.string.failure_target_embedding_failed
        LocalGameFailureCode.GUESS_EMBEDDING_FAILED -> Res.string.failure_guess_embedding_failed
        LocalGameFailureCode.GAME_NOT_FOUND -> Res.string.failure_game_not_found
        LocalGameFailureCode.THING_NOT_FOUND -> Res.string.failure_thing_not_found
        LocalGameFailureCode.MATCH_POLICY_INVALID -> Res.string.failure_match_policy_invalid
        LocalGameFailureCode.PERSISTENCE_FAILED -> Res.string.failure_persistence_failed
    },
)
