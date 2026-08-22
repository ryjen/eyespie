package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.features.clueauthoring.ClueAuthoringFactory
import com.micrantha.eyespie.features.create.CreateGameFactory
import com.micrantha.eyespie.features.gamedetail.GameDetailFactory
import com.micrantha.eyespie.features.home.HomeFactory
import com.micrantha.eyespie.features.onboarding.OnboardingFactory
import com.micrantha.eyespie.features.play.PlayGameFactory

class AppGraph(
    val navigator: AppNavigator,
    val homeFactory: HomeFactory,
    val onboardingFactory: OnboardingFactory,
    val createGameFactory: CreateGameFactory,
    val gameDetailFactory: GameDetailFactory,
    val clueAuthoringFactory: ClueAuthoringFactory,
    val playGameFactory: PlayGameFactory,
)
