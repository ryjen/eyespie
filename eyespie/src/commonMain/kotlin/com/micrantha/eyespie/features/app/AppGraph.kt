package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.features.create.CreateGameInteractor
import com.micrantha.eyespie.features.home.HomeIntent
import com.micrantha.eyespie.features.home.HomeInteractor
import com.micrantha.eyespie.features.onboarding.OnboardingInteractor
import com.micrantha.eyespie.features.play.PlayGameInteractor
import com.micrantha.eyespie.features.play.PlayGameState
import com.micrantha.eyespie.game.EyespieRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AppNavigator {
    val screen: StateFlow<AppScreen>
    fun navigate(screen: AppScreen)
}

class StateFlowAppNavigator(
    initialScreen: AppScreen = AppScreen.Home,
) : AppNavigator {
    private val mutableScreen = MutableStateFlow(initialScreen)
    override val screen: StateFlow<AppScreen> = mutableScreen.asStateFlow()

    override fun navigate(screen: AppScreen) {
        mutableScreen.value = screen
    }
}

class AppGraph(
    private val useCases: AppGameUseCases,
    private val scope: CoroutineScope,
    val navigator: AppNavigator = StateFlowAppNavigator(),
) {
    val homeInteractor: HomeInteractor = HomeInteractor(
        useCases = useCases,
        scope = scope,
        navigate = navigator::navigate,
    )

    val onboardingInteractor: OnboardingInteractor = OnboardingInteractor(
        navigate = navigator::navigate,
    )

    val createGameInteractor: CreateGameInteractor = CreateGameInteractor(
        useCases = useCases,
        scope = scope,
        navigate = navigator::navigate,
        adoptSnapshot = { homeInteractor.dispatch(HomeIntent.AdoptSnapshot(it)) },
        reportHomeFailure = { homeInteractor.dispatch(HomeIntent.OperationFailed(it)) },
    )

    fun playGameInteractor(screen: AppScreen.Play): PlayGameInteractor? {
        val game = homeInteractor.state.value.snapshot?.games?.firstOrNull { it.id == screen.gameId }
            ?: return null
        val thing = game.things.firstOrNull { it.id == screen.thingId }
            ?: return null

        return PlayGameInteractor(
            useCases = useCases,
            scope = scope,
            navigate = navigator::navigate,
            adoptSnapshot = { homeInteractor.dispatch(HomeIntent.AdoptSnapshot(it)) },
            initialState = PlayGameState(game = game, thing = thing),
        )
    }

    fun start() {
        homeInteractor.dispatch(HomeIntent.Refresh)
    }

    companion object {
        fun fromRuntime(
            runtime: EyespieRuntime,
            scope: CoroutineScope,
            navigator: AppNavigator = StateFlowAppNavigator(),
        ): AppGraph = AppGraph(
            useCases = RuntimeAppGameUseCases(runtime),
            scope = scope,
            navigator = navigator,
        )
    }
}
