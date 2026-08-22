package com.micrantha.eyespie

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micrantha.eyespie.features.app.AppGraphFactory
import com.micrantha.eyespie.features.app.AppRoute
import com.micrantha.eyespie.features.create.CreateGameRoute
import com.micrantha.eyespie.features.gamedetail.GameDetailRoute
import com.micrantha.eyespie.features.home.HomeRoute
import com.micrantha.eyespie.features.onboarding.OnboardingRoute
import com.micrantha.eyespie.features.play.PlayGameRoute
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.sharing.GameDocumentTransfer

@Composable
fun App(
    runtime: EyespieRuntime,
    documentTransfer: GameDocumentTransfer? = null,
) {
    val graph = remember(runtime, documentTransfer) {
        AppGraphFactory.fromRuntime(runtime, documentTransfer = documentTransfer)
    }
    val route by graph.navigator.route.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when (val current = route) {
                        AppRoute.Home -> HomeRoute(graph.homeFactory)
                        AppRoute.Onboarding -> OnboardingRoute(graph.onboardingFactory)
                        AppRoute.Create -> CreateGameRoute(graph.createGameFactory)
                        is AppRoute.GameDetail -> GameDetailRoute(
                            factory = graph.gameDetailFactory,
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
