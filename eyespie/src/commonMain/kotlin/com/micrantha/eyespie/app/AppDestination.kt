package com.micrantha.eyespie.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.features.clueauthoring.ClueAuthoringRoute
import com.micrantha.eyespie.features.create.CreateGameRoute
import com.micrantha.eyespie.features.gamedetail.GameDetailRoute
import com.micrantha.eyespie.features.home.HomeRoute
import com.micrantha.eyespie.features.onboarding.OnboardingRoute
import com.micrantha.eyespie.features.play.PlayGameRoute
import com.micrantha.eyespie.features.utility.UtilityRoute

internal val LocalAppGraph = staticCompositionLocalOf<AppGraph> { error("AppGraph is not provided") }
internal val LocalAppMessageSink = staticCompositionLocalOf<suspend (String) -> Unit> {
    error("App message sink is not provided")
}

/** Authoring capture/review destinations own safe-area chrome so their visual field can be edge-to-edge. */
internal interface FullBleedDestination

internal fun AppRoute.toDestination(): Screen = when (this) {
    AppRoute.Home -> HomeDestination
    AppRoute.Onboarding -> OnboardingDestination
    AppRoute.Utility -> UtilityDestination
    AppRoute.Create -> CreateDestination
    is AppRoute.GameDetail -> GameDetailDestination(gameId.value)
    is AppRoute.ClueAuthoring -> ClueAuthoringDestination(gameId.value)
    is AppRoute.Play -> PlayDestination(gameId.value, thingId.value)
}

private data object HomeDestination : Screen {
    override val key: ScreenKey = "home"

    @Composable
    override fun Content() {
        HomeRoute(LocalAppGraph.current.homeFactory, LocalAppMessageSink.current)
    }
}

private data object OnboardingDestination : Screen {
    override val key: ScreenKey = "onboarding"

    @Composable
    override fun Content() {
        OnboardingRoute(LocalAppGraph.current.onboardingFactory)
    }
}

private data object UtilityDestination : Screen {
    override val key: ScreenKey = "utility"

    @Composable
    override fun Content() {
        UtilityRoute(LocalAppGraph.current.utilityFactory)
    }
}

private data object CreateDestination : Screen, FullBleedDestination {
    override val key: ScreenKey = "create"

    @Composable
    override fun Content() {
        CreateGameRoute(LocalAppGraph.current.createGameFactory)
    }
}

private data class GameDetailDestination(
    private val gameIdValue: String,
) : Screen {
    override val key: ScreenKey = "game-detail:$gameIdValue"

    @Composable
    override fun Content() {
        GameDetailRoute(
            factory = LocalAppGraph.current.gameDetailFactory,
            gameId = GameId(gameIdValue),
            onMessage = LocalAppMessageSink.current,
        )
    }
}

private data class ClueAuthoringDestination(
    private val gameIdValue: String,
) : Screen, FullBleedDestination {
    override val key: ScreenKey = "clue-authoring:$gameIdValue"

    @Composable
    override fun Content() {
        ClueAuthoringRoute(
            factory = LocalAppGraph.current.clueAuthoringFactory,
            gameId = GameId(gameIdValue),
        )
    }
}

private data class PlayDestination(
    private val gameIdValue: String,
    private val thingIdValue: String,
) : Screen {
    override val key: ScreenKey = "play:$gameIdValue:$thingIdValue"

    @Composable
    override fun Content() {
        PlayGameRoute(
            factory = LocalAppGraph.current.playGameFactory,
            gameId = GameId(gameIdValue),
            thingId = ThingId(thingIdValue),
        )
    }
}
