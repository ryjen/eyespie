package com.micrantha.eyespie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.features.app.AppGraph
import com.micrantha.eyespie.features.app.AppScreen
import com.micrantha.eyespie.game.EyespieRuntime

@Composable
fun App(runtime: EyespieRuntime) {
    val scope = rememberCoroutineScope()
    val graph = remember(runtime, scope) { AppGraph.fromRuntime(runtime, scope) }

    val screen by graph.navigator.screen.collectAsState()
    val homeState by graph.homeInteractor.state.collectAsState()
    val onboardingState by graph.onboardingInteractor.state.collectAsState()
    val createState by graph.createGameInteractor.state.collectAsState()

    LaunchedEffect(graph) {
        graph.start()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Eyespie", style = MaterialTheme.typography.headlineLarge)
                Text("Offline travel-spy game", style = MaterialTheme.typography.titleMedium)

                Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when (val current = screen) {
                        AppScreen.Home -> HomeView(homeState, graph.homeInteractor::dispatch)
                        AppScreen.Onboarding -> OnboardingView(
                            onboardingState,
                            graph.onboardingInteractor::dispatch,
                        )
                        AppScreen.Create -> CreateGameView(
                            createState,
                            graph.createGameInteractor::dispatch,
                        )
                        is AppScreen.Play -> {
                            val playInteractor = remember(current, graph, homeState.snapshot) {
                                graph.playGameInteractor(current)
                            }
                            if (playInteractor == null) {
                                Text("This local game is no longer available.")
                                Button(onClick = { graph.navigator.navigate(AppScreen.Home) }) {
                                    Text("Back")
                                }
                            } else {
                                val playState by playInteractor.state.collectAsState()
                                PlayGameView(playState, playInteractor::dispatch)
                            }
                        }
                    }
                }
            }
        }
    }
}
