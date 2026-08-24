package com.micrantha.eyespie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.features.app.AppGraphFactory
import com.micrantha.eyespie.features.app.AppRoute
import com.micrantha.eyespie.features.app.StateFlowAppNavigator
import com.micrantha.eyespie.features.clueauthoring.ClueAuthoringRoute
import com.micrantha.eyespie.features.create.CreateGameRoute
import com.micrantha.eyespie.features.gamedetail.GameDetailRoute
import com.micrantha.eyespie.features.home.HomeRoute
import com.micrantha.eyespie.features.onboarding.OnboardingRoute
import com.micrantha.eyespie.features.play.PlayGameRoute
import com.micrantha.eyespie.features.utility.UtilityRoute
import com.micrantha.eyespie.game.EyespieRuntime
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

    MaterialTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { scaffoldPadding ->
            Surface(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
                val completed = onboardingCompleted
                if (completed == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Eyespie", style = MaterialTheme.typography.headlineLarge)
                        Text("Loading local game…", style = MaterialTheme.typography.titleMedium)
                        CircularProgressIndicator()
                    }
                } else {
                    val graph = remember(runtime, documentTransfer, completed) {
                        AppGraphFactory.fromRuntime(
                            runtime = runtime,
                            navigator = StateFlowAppNavigator(
                                if (completed) AppRoute.Home else AppRoute.Onboarding,
                            ),
                            documentTransfer = documentTransfer,
                        )
                    }
                    val route by graph.navigator.route.collectAsState()
                    val showMessage: suspend (String) -> Unit = { message ->
                        snackbarHostState.showSnackbar(message)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Eyespie", style = MaterialTheme.typography.headlineLarge)
                        Text("Offline travel-spy game", style = MaterialTheme.typography.titleMedium)

                        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            when (val current = route) {
                                AppRoute.Home -> HomeRoute(graph.homeFactory, showMessage)
                                AppRoute.Onboarding -> OnboardingRoute(graph.onboardingFactory)
                                AppRoute.Utility -> UtilityRoute(graph.utilityFactory)
                                AppRoute.Create -> CreateGameRoute(graph.createGameFactory)
                                is AppRoute.GameDetail -> GameDetailRoute(
                                    factory = graph.gameDetailFactory,
                                    gameId = current.gameId,
                                    onMessage = showMessage,
                                )
                                is AppRoute.ClueAuthoring -> ClueAuthoringRoute(
                                    factory = graph.clueAuthoringFactory,
                                    gameId = current.gameId,
                                )
                                is AppRoute.Play -> PlayGameRoute(
                                    factory = graph.playGameFactory,
                                    gameId = current.gameId,
                                    thingId = current.thingId,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
