package com.micrantha.eyespie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.micrantha.eyespie.app.LocalAppGraph
import com.micrantha.eyespie.app.LocalAppMessageSink
import com.micrantha.eyespie.app.VoyagerAppNavigation
import com.micrantha.eyespie.app.toDestination
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.presentation.theme.EyespieLogo
import com.micrantha.eyespie.presentation.theme.EyespieTheme
import com.micrantha.eyespie.sharing.GameDocumentTransfer

@Composable
fun App(
    runtime: EyespieRuntime,
    documentTransfer: GameDocumentTransfer? = null,
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
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
                color = MaterialTheme.colorScheme.background,
            ) {
                val completed = onboardingCompleted
                if (completed == null) {
                    LoadingLocalGame()
                } else {
                    val navigation = remember(runtime, documentTransfer) { AppNavigationBridge() }
                    val graph = remember(runtime, documentTransfer, navigation) {
                        AppGraphFactory.fromRuntime(
                            runtime = runtime,
                            navigation = navigation,
                            documentTransfer = documentTransfer,
                        )
                    }
                    val showMessage: suspend (String) -> Unit = remember(snackbarHostState) {
                        { message -> snackbarHostState.showSnackbar(message) }
                    }
                    val initialRoute = if (completed) AppRoute.Home else AppRoute.Onboarding
                    val initialDestination = remember(initialRoute) { initialRoute.toDestination() }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(horizontal = 16.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            Navigator(initialDestination) { navigator ->
                                val voyagerNavigation = remember(navigator) {
                                    VoyagerAppNavigation(navigator)
                                }
                                DisposableEffect(navigation, voyagerNavigation) {
                                    navigation.attach(voyagerNavigation)
                                    onDispose { navigation.detach(voyagerNavigation) }
                                }

                                val currentScreen = navigator.lastItem
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
