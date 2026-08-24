package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.generated.resources.*
import org.jetbrains.compose.resources.StringResource

internal fun homeImportMessageResource(result: HomeImportResult): StringResource? = when (result) {
    HomeImportResult.Imported -> Res.string.feedback_game_imported
    HomeImportResult.AlreadyPresent -> Res.string.feedback_game_already_present
    HomeImportResult.Conflict -> Res.string.failure_import_conflict
    HomeImportResult.InvalidFile -> Res.string.failure_import_invalid_file
    HomeImportResult.TooLarge -> Res.string.failure_import_too_large
    HomeImportResult.Busy -> Res.string.failure_document_busy
    HomeImportResult.Failed -> Res.string.failure_import_failed
    HomeImportResult.Unavailable -> Res.string.failure_import_unavailable
    HomeImportResult.Cancelled -> null
}
