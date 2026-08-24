package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.generated.resources.*
import org.jetbrains.compose.resources.StringResource

internal fun gameDetailShareMessageResource(result: GameDetailShareResult): StringResource? = when (result) {
    GameDetailShareResult.Shared -> Res.string.feedback_game_shared
    GameDetailShareResult.NotLocalCreator -> Res.string.failure_share_not_local_creator
    GameDetailShareResult.TooLarge -> Res.string.failure_share_too_large
    GameDetailShareResult.Busy -> Res.string.failure_document_busy
    GameDetailShareResult.Failed -> Res.string.failure_share_failed
    GameDetailShareResult.Unavailable -> Res.string.failure_share_unavailable
    GameDetailShareResult.Cancelled -> null
}
