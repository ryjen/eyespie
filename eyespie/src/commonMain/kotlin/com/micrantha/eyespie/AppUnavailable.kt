package com.micrantha.eyespie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.features.utility.DiagnosticExportResult
import com.micrantha.eyespie.features.utility.DiagnosticsExporter
import com.micrantha.eyespie.mvi.EffectEmitter
import com.micrantha.eyespie.presentation.theme.EyespieSecondaryAction
import com.micrantha.eyespie.ui.EyespieTheme
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

private sealed interface AppUnavailableEffect {
    data class DiagnosticExportFinished(val result: DiagnosticExportResult) : AppUnavailableEffect
}

@Composable
fun AppUnavailable(
    diagnosticsExporter: DiagnosticsExporter? = null,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val effectEmitter = remember { EffectEmitter<AppUnavailableEffect>() }
    var exporting by remember { mutableStateOf(false) }

    LaunchedEffect(effectEmitter, snackbarHostState) {
        effectEmitter.effects.collect { effect ->
            when (effect) {
                is AppUnavailableEffect.DiagnosticExportFinished -> {
                    snackbarHostState.showSnackbar(bootstrapDiagnosticExportMessage(effect.result))
                }
            }
        }
    }

    EyespieTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { scaffoldPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Eyespie",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("The local game runtime could not be initialized.")
                    Text(
                        "Verify required local components and relaunch the app.",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    diagnosticsExporter?.let { exporter ->
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "You can export bounded support diagnostics for this startup failure. The export does not include game images, embeddings, clue or answer text, keys, contacts, exact location, private paths, or the exception message.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        EyespieSecondaryAction(
                            text = if (exporting) "Exporting diagnostics…" else "Export diagnostics",
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !exporting,
                            onClick = {
                                if (!exporting) {
                                    exporting = true
                                    scope.launch {
                                        val result = try {
                                            exporter.export()
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                            DiagnosticExportResult.Failed
                                        } finally {
                                            exporting = false
                                        }
                                        effectEmitter.emit(
                                            AppUnavailableEffect.DiagnosticExportFinished(result),
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun bootstrapDiagnosticExportMessage(result: DiagnosticExportResult): String = when (result) {
    DiagnosticExportResult.Exported -> "Diagnostics exported."
    DiagnosticExportResult.Cancelled -> "Diagnostics export cancelled."
    DiagnosticExportResult.Busy -> "Another document operation is already active."
    DiagnosticExportResult.TooLarge -> "Diagnostics exceeded the supported export size."
    DiagnosticExportResult.Failed -> "Diagnostics could not be exported."
    DiagnosticExportResult.Unavailable -> "Diagnostics export is unavailable on this platform."
}
