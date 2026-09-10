package com.micrantha.eyespie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.micrantha.eyespie.app.AppGraphFactory
import com.micrantha.eyespie.app.AppNavigationBridge
import com.micrantha.eyespie.app.AppRoute
import com.micrantha.eyespie.app.ExternalAppIntentResult
import com.micrantha.eyespie.app.ExternalAppIntentSource
import com.micrantha.eyespie.app.ExternalDocumentLandingDestination
import com.micrantha.eyespie.app.ExternalIngressGateDestination
import com.micrantha.eyespie.app.FullBleedDestination
import com.micrantha.eyespie.app.LocalAppGraph
import com.micrantha.eyespie.app.LocalAppMessageSink
import com.micrantha.eyespie.app.VoyagerAppNavigation
import com.micrantha.eyespie.app.toDestination
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.generated.resources.Res
import com.micrantha.eyespie.generated.resources.failure_deep_link_failed
import com.micrantha.eyespie.generated.resources.failure_deep_link_game_not_found
import com.micrantha.eyespie.presentation.theme.EyespieLogo
import com.micrantha.eyespie.presentation.theme.EyespieTheme
import com.micrantha.eyespie.sharing.ExternalGameDocumentSource
import com.micrantha.eyespie.sharing.GameDocumentTransfer
import com.micrantha.eyespie.sharing.GameSharePresenter
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.getString

@Composable
fun App(
    runtime: EyespieRuntime,
    documentTransfer: GameDocumentTransfer? = null,
    externalDocumentSource: ExternalGameDocumentSource? = null,
    sharePresenter: GameSharePresenter? = null,
    externalAppIntentSource: ExternalAppIntentSource? = null,
) {
    val onboardingCompleted by produceState<Boolean?>(null, runtime) {
        value = try {
            runtime.onboardingPreferences.isCompleted()
        } catch (_: Exception) {
            false
        }
    }

    EyespieTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { scaffoldPadding ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                val completed = onboardingCompleted
                if (completed == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(scaffoldPadding),
                    ) {
                        LoadingLocalGame()
                    }
                } else {
                    val navigation = remember(
                        runtime,
                        documentTransfer,
                        externalDocumentSource,
                        sharePresenter,
                        externalAppIntentSource,
                    ) { AppNavigationBridge() }
                    val graph = remember(
                        runtime,
                        documentTransfer,
                        externalDocumentSource,
                        sharePresenter,
                        externalAppIntentSource,
                        navigation,
                    ) {
                        AppGraphFactory.fromRuntime(
                            runtime = runtime,
                            navigation = navigation,
                            documentTransfer = documentTransfer,
                            externalDocumentSource = externalDocumentSource,
                            sharePresenter = sharePresenter,
                        )
                    }
                    val showMessage: suspend (String) -> Unit = remember(snackbarHostState) {
                        { message -> snackbarHostState.showSnackbar(message) }
                    }
                    val initialRoute = if (completed) AppRoute.Home else AppRoute.Onboarding
                    val initialDestination = remember(initialRoute) { initialRoute.toDestination() }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Navigator(initialDestination) { navigator ->
                            val voyagerNavigation = remember(navigator) {
                                VoyagerAppNavigation(navigator)
                            }
                            DisposableEffect(navigation, voyagerNavigation) {
                                navigation.attach(voyagerNavigation)
                                onDispose { navigation.detach(voyagerNavigation) }
                            }

                            val currentScreen = navigator.lastItem

                            LaunchedEffect(externalDocumentSource, currentScreen, voyagerNavigation) {
                                if (
                                    currentScreen !is ExternalIngressGateDestination &&
                                    currentScreen !is ExternalDocumentLandingDestination
                                ) {
                                    externalDocumentSource
                                        ?.pending
                                        ?.filter { it }
                                        ?.collect {
                                            voyagerNavigation.replaceAll(AppRoute.Home)
                                        }
                                }
                            }

                            LaunchedEffect(externalAppIntentSource, currentScreen, graph) {
                                if (currentScreen !is ExternalIngressGateDestination) {
                                    val source = externalAppIntentSource ?: return@LaunchedEffect
                                    source.intents.collect { intent ->
                                        when (graph.externalAppIntentHandler.handle(intent)) {
                                            ExternalAppIntentResult.Opened -> Unit
                                            ExternalAppIntentResult.NotFound -> showMessage(
                                                getString(Res.string.failure_deep_link_game_not_found),
                                            )
                                            ExternalAppIntentResult.Failed -> showMessage(
                                                getString(Res.string.failure_deep_link_failed),
                                            )
                                        }
                                        // Reaching here means handling (including any user-visible
                                        // failure message) completed without lifecycle cancellation.
                                        source.acknowledge(intent)
                                    }
                                }
                            }

                            val destinationModifier = if (currentScreen is FullBleedDestination) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier
                                    .fillMaxSize()
                                    .padding(scaffoldPadding)
                                    .safeDrawingPadding()
                                    .padding(horizontal = 16.dp)
                            }

                            Box(modifier = destinationModifier) {
                                navigator.saveableState("currentScreen", currentScreen) {
                                    CompositionLocalProvider(
                                        LocalAppGraph provides graph,
                                        LocalAppMessageSink provides showMessage,
                                    ) {
                                        currentScreen.Content()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingLocalGame() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EyespieLogo(size = 72.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Eyespie",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineLarge,
        )
        Text("Loading local game…", style = MaterialTheme.typography.titleMedium)
        CircularProgressIndicator()
    }
}
