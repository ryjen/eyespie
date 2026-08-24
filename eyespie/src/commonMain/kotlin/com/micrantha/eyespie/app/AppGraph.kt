package com.micrantha.eyespie.app

import com.micrantha.eyespie.features.clueauthoring.ClueAuthoringFactory
import com.micrantha.eyespie.features.create.CreateGameFactory
import com.micrantha.eyespie.features.gamedetail.GameDetailFactory
import com.micrantha.eyespie.features.home.HomeFactory
import com.micrantha.eyespie.features.onboarding.OnboardingFactory
import com.micrantha.eyespie.features.play.PlayGameFactory
import com.micrantha.eyespie.features.utility.UtilityFactory

class AppGraph(
    val homeFactory: HomeFactory,
    val onboardingFactory: OnboardingFactory,
    val utilityFactory: UtilityFactory,
    val createGameFactory: CreateGameFactory,
    val gameDetailFactory: GameDetailFactory,
    val clueAuthoringFactory: ClueAuthoringFactory,
    val playGameFactory: PlayGameFactory,
)
