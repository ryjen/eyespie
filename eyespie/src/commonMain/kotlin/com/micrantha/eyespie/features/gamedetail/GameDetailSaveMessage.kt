package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.generated.resources.*
import org.jetbrains.compose.resources.StringResource

internal fun gameDetailSaveMessageResource(result: GameDetailSaveResult): StringResource? = when (result) {
    GameDetailSaveResult.Saved -> Res.string.feedback_game_saved
    GameDetailSaveResult.NotLocalCreator -> Res.string.failure_share_not_local_creator
    GameDetailSaveResult.TooLarge -> Res.string.failure_share_too_large
    GameDetailSaveResult.Busy -> Res.string.failure_document_busy
    GameDetailSaveResult.Failed -> Res.string.failure_save_failed
    GameDetailSaveResult.Unavailable -> Res.string.failure_save_unavailable
    GameDetailSaveResult.Cancelled -> null
}
