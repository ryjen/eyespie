package com.micrantha.eyespie.sharing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AndroidBundleActions(
    runtime: EyespieRuntime,
    transfer: GameDocumentTransfer,
    onImported: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var games by remember(runtime) { mutableStateOf<List<LocalGameSummary>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    suspend fun refreshGames() {
        when (val result = runtime.gameLoop.loadSnapshot()) {
            is LocalGameResult.Success -> games = result.value.games
            is LocalGameResult.Failure -> status = "Local games could not be loaded."
        }
    }

    LaunchedEffect(runtime) {
        refreshGames()
    }

    Surface(
        tonalElevation = 4.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        status = null
                        try {
                            status = when (val read = transfer.read()) {
                                is GameDocumentReadResult.Success -> when (
                                    val imported = runtime.bundleService.import(read.bytes)
                                ) {
                                    is GameBundleImportResult.Imported -> {
                                        refreshGames()
                                        onImported()
                                        "Game imported."
                                    }
                                    is GameBundleImportResult.AlreadyPresent -> {
                                        refreshGames()
                                        onImported()
                                        "Game already present."
                                    }
                                    is GameBundleImportResult.Conflict ->
                                        "A different local game already uses this game ID."
                                    is GameBundleImportResult.InvalidFormat ->
                                        "The selected file is not a supported Eyespie game."
                                    is GameBundleImportResult.Failure ->
                                        "The selected game could not be verified or imported."
                                }
                                GameDocumentReadResult.Cancelled -> null
                                GameDocumentReadResult.Busy -> "Another document operation is already running."
                                GameDocumentReadResult.TooLarge -> "The selected Eyespie file is too large."
                                GameDocumentReadResult.Failed -> "The selected document could not be read."
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            status = "The selected game could not be imported."
                        } finally {
                            busy = false
                        }
                    }
                },
            ) {
                Text(if (busy) "Working…" else "Import .eyespie")
            }

            games.forEach { game ->
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            status = null
                            try {
                                status = when (val exported = runtime.bundleService.export(game.id)) {
                                    is GameBundleExportResult.Success -> when (
                                        transfer.write(
                                            suggestedGameBundleFileName(game.name, game.id.value),
                                            exported.bytes,
                                        )
                                    ) {
                                        GameDocumentWriteResult.Success -> "Game exported."
                                        GameDocumentWriteResult.Cancelled -> null
                                        GameDocumentWriteResult.Busy -> "Another document operation is already running."
                                        GameDocumentWriteResult.TooLarge -> "The Eyespie game is too large to export."
                                        GameDocumentWriteResult.Failed -> "The Eyespie game could not be written."
                                    }
                                    is GameBundleExportResult.Failure -> when (exported.code) {
                                        GameBundleExportFailureCode.NOT_LOCAL_CREATOR ->
                                            "Only games authored by this local identity can be exported."
                                        else -> "This local game cannot be exported."
                                    }
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                status = "This local game cannot be exported."
                            } finally {
                                busy = false
                            }
                        }
                    },
                ) {
                    Text("Export ${game.name}")
                }
            }

            status?.let { Text(it) }
        }
    }
}
