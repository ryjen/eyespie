package com.micrantha.eyespie.features

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.features.create.CreateGameIntent
import com.micrantha.eyespie.features.create.CreateGameScreen
import com.micrantha.eyespie.features.create.CreateGameState
import com.micrantha.eyespie.features.home.HomeContent
import com.micrantha.eyespie.features.home.HomeIntent
import com.micrantha.eyespie.features.home.HomeScreen
import com.micrantha.eyespie.features.home.HomeState
import com.micrantha.eyespie.features.onboarding.OnboardingIntent
import com.micrantha.eyespie.features.onboarding.OnboardingScreen
import com.micrantha.eyespie.features.onboarding.OnboardingState
import com.micrantha.eyespie.features.play.PlayGameContent
import com.micrantha.eyespie.features.play.PlayGameIntent
import com.micrantha.eyespie.features.play.PlayGameScreen
import com.micrantha.eyespie.features.play.PlayGameState
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeatureScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun home_screen_dispatches_create_intent() {
        val intents = mutableListOf<HomeIntent>()
        compose.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeState(
                        content = HomeContent("Agent", "player-1", emptyList()),
                        loading = false,
                    ),
                    dispatch = intents::add,
                )
            }
        }

        compose.onNodeWithText("Create game").performClick()

        assertEquals(listOf(HomeIntent.CreateSelected), intents)
    }

    @Test
    fun onboarding_screen_dispatches_next_intent() {
        val intents = mutableListOf<OnboardingIntent>()
        compose.setContent {
            MaterialTheme {
                OnboardingScreen(
                    state = OnboardingState(),
                    dispatch = intents::add,
                )
            }
        }

        compose.onNodeWithText("Next").performClick()

        assertEquals(listOf(OnboardingIntent.Next), intents)
    }

    @Test
    fun create_screen_dispatches_back_intent_without_resolving_dependencies() {
        val intents = mutableListOf<CreateGameIntent>()
        compose.setContent {
            MaterialTheme {
                CreateGameScreen(
                    state = CreateGameState(),
                    dispatch = intents::add,
                )
            }
        }

        compose.onNodeWithText("Back").performClick()

        assertEquals(listOf(CreateGameIntent.Back), intents)
    }

    @Test
    fun play_screen_dispatches_back_intent_without_resolving_dependencies() {
        val intents = mutableListOf<PlayGameIntent>()
        compose.setContent {
            MaterialTheme {
                PlayGameScreen(
                    state = PlayGameState(
                        gameId = GameId("game-1"),
                        thingId = ThingId("thing-1"),
                        content = PlayGameContent(
                            gameName = "Trip",
                            clueText = "Find it",
                            matched = false,
                            bestSimilarity = null,
                        ),
                        loading = false,
                    ),
                    dispatch = intents::add,
                )
            }
        }

        compose.onNodeWithText("Back").performClick()

        assertEquals(listOf(PlayGameIntent.Back), intents)
    }
}
