package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.features.clueauthoring.ClueAuthoringOutput
import com.micrantha.eyespie.features.create.CreateGameOutput
import com.micrantha.eyespie.features.gamedetail.GameDetailOutput
import com.micrantha.eyespie.features.home.HomeOutput
import com.micrantha.eyespie.features.onboarding.OnboardingOutput
import com.micrantha.eyespie.features.play.PlayGameOutput
import com.micrantha.eyespie.features.utility.UtilityOutput
import com.micrantha.eyespie.testsupport.testGameId
import com.micrantha.eyespie.testsupport.testThingId
import kotlin.test.Test
import kotlin.test.assertEquals

class AppCoordinatorTest {
    @Test
    fun forward_outputs_push_product_destinations() {
        val navigation = RecordingNavigation()
        val coordinator = AppCoordinator(navigation)

        coordinator.onHomeOutput(HomeOutput.CreateRequested)
        coordinator.onHomeOutput(HomeOutput.UtilityRequested)
        coordinator.onUtilityOutput(UtilityOutput.OnboardingRequested)
        coordinator.onHomeOutput(HomeOutput.GameRequested(testGameId))
        coordinator.onGameDetailOutput(GameDetailOutput.PlayRequested(testGameId, testThingId))

        assertEquals(
            listOf<NavigationCommand>(
                NavigationCommand.Push(AppRoute.Create),
                NavigationCommand.Push(AppRoute.Utility),
                NavigationCommand.Push(AppRoute.Onboarding),
                NavigationCommand.Push(AppRoute.GameDetail(testGameId)),
                NavigationCommand.Push(AppRoute.Play(testGameId, testThingId)),
            ),
            navigation.commands,
        )
    }

    @Test
    fun terminal_outputs_use_stack_semantics() {
        val navigation = RecordingNavigation()
        val coordinator = AppCoordinator(navigation)

        coordinator.onCreateGameOutput(CreateGameOutput.Cancelled)
        coordinator.onUtilityOutput(UtilityOutput.Closed)
        coordinator.onGameDetailOutput(GameDetailOutput.Closed)
        coordinator.onClueAuthoringOutput(ClueAuthoringOutput.Closed(testGameId))
        coordinator.onPlayGameOutput(PlayGameOutput.Closed(testGameId))
        coordinator.onOnboardingOutput(OnboardingOutput.Completed)
        coordinator.onPlayGameOutput(PlayGameOutput.Advance(testGameId, testThingId))

        assertEquals(
            listOf<NavigationCommand>(
                NavigationCommand.Pop,
                NavigationCommand.Pop,
                NavigationCommand.Pop,
                NavigationCommand.Pop,
                NavigationCommand.Pop,
                NavigationCommand.ReplaceAll(AppRoute.Home),
                NavigationCommand.Replace(AppRoute.Play(testGameId, testThingId)),
            ),
            navigation.commands,
        )
    }
}

private sealed interface NavigationCommand {
    data class Push(val route: AppRoute) : NavigationCommand
    data class Replace(val route: AppRoute) : NavigationCommand
    data class ReplaceAll(val route: AppRoute) : NavigationCommand
    data object Pop : NavigationCommand
}

private class RecordingNavigation : AppNavigation {
    val commands = mutableListOf<NavigationCommand>()

    override fun push(route: AppRoute) {
        commands += NavigationCommand.Push(route)
    }

    override fun replace(route: AppRoute) {
        commands += NavigationCommand.Replace(route)
    }

    override fun replaceAll(route: AppRoute) {
        commands += NavigationCommand.ReplaceAll(route)
    }

    override fun pop() {
        commands += NavigationCommand.Pop
    }
}
