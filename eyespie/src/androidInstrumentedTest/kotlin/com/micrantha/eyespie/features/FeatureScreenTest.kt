package com.micrantha.eyespie.features

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.features.clueauthoring.ClueAuthoringIntent
import com.micrantha.eyespie.features.clueauthoring.ClueAuthoringScreen
import com.micrantha.eyespie.features.clueauthoring.ClueAuthoringState
import com.micrantha.eyespie.features.create.CreateGameIntent
import com.micrantha.eyespie.features.create.CreateGameScreen
import com.micrantha.eyespie.features.create.CreateGameState
import com.micrantha.eyespie.features.gamedetail.GameDetailContent
import com.micrantha.eyespie.features.gamedetail.GameDetailIntent
import com.micrantha.eyespie.features.gamedetail.GameDetailScreen
import com.micrantha.eyespie.features.gamedetail.GameDetailState
import com.micrantha.eyespie.features.home.HomeContent
import com.micrantha.eyespie.features.home.HomeImportPreview
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
import com.micrantha.eyespie.features.utility.UtilityContent
import com.micrantha.eyespie.features.utility.UtilityIntent
import com.micrantha.eyespie.features.utility.UtilityScreen
import com.micrantha.eyespie.features.utility.UtilityState
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.presentation.LocalCameraCaptureSurfaceOverride
import com.micrantha.eyespie.presentation.theme.EyespieTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
            EyespieTheme {
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
    fun home_import_preview_dispatches_confirm_intent() {
        val intents = mutableListOf<HomeIntent>()
        compose.setContent {
            EyespieTheme {
                HomeScreen(
                    state = HomeState(
                        content = HomeContent("Agent", "player-1", emptyList()),
                        loading = false,
                        importPreview = HomeImportPreview(
                            gameName = "Road Trip",
                            clueCount = 3,
                            creatorIdSuffix = "creator12345",
                            gameIdSuffix = "game12345678",
                        ),
                    ),
                    dispatch = intents::add,
                )
            }
        }

        compose.onNodeWithText("Add game").performScrollTo().performClick()

        assertEquals(listOf(HomeIntent.ImportConfirmed), intents)
    }

    @Test
    fun home_import_preview_dispatches_cancel_intent() {
        val intents = mutableListOf<HomeIntent>()
        compose.setContent {
            EyespieTheme {
                HomeScreen(
                    state = HomeState(
                        content = HomeContent("Agent", "player-1", emptyList()),
                        loading = false,
                        importPreview = HomeImportPreview(
                            gameName = "Road Trip",
                            clueCount = 1,
                            creatorIdSuffix = "creator12345",
                            gameIdSuffix = "game12345678",
                        ),
                    ),
                    dispatch = intents::add,
                )
            }
        }

        compose.onNodeWithText("Cancel").performScrollTo().performClick()

        assertEquals(listOf(HomeIntent.ImportPreviewCancelled), intents)
    }

    @Test
    fun onboarding_screen_dispatches_next_intent() {
        val intents = mutableListOf<OnboardingIntent>()
        compose.setContent {
            EyespieTheme {
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
            EyespieTheme {
                CreateGameScreen(
                    state = CreateGameState(),
                    dispatch = intents::add,
                )
            }
        }

        compose.onNodeWithText("Back to field desk").performClick()

        assertEquals(listOf(CreateGameIntent.Back), intents)
    }

    @Test
    fun create_screen_disables_back_navigation_while_busy() {
        compose.setContent {
            EyespieTheme {
                CreateGameScreen(
                    state = CreateGameState(busy = true),
                    dispatch = {},
                )
            }
        }

        compose.onNodeWithText("Back to field desk").assertIsNotEnabled()
    }

    @Test
    fun create_screen_hides_form_until_capture_and_commits_reviewed_image() {
        val intents = mutableListOf<CreateGameIntent>()
        compose.setContent {
            EyespieTheme {
                FakeCameraCaptureSurface {
                    CreateGameScreen(
                        state = CreateGameState(
                            name = "Road Trip",
                            clue = "Find the marker",
                            expectedAnswer = "Marker",
                        ),
                        dispatch = intents::add,
                    )
                }
            }
        }

        compose.onNodeWithText("Game name").assertDoesNotExist()
        compose.onNodeWithContentDescription("Capture target").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Game name").assertExists()
        assertTrue(intents.isEmpty())

        compose.onNodeWithText("Create game").performScrollTo().performClick()
        assertEquals(1, intents.size)
        assertTrue(intents.single() is CreateGameIntent.TargetCaptured)
    }

    @Test
    fun game_detail_screen_dispatches_add_clue_intent() {
        val intents = mutableListOf<GameDetailIntent>()
        compose.setContent {
            EyespieTheme {
                GameDetailScreen(
                    state = GameDetailState(
                        content = GameDetailContent(
                            name = "Trip",
                            things = emptyList(),
                            localCreator = true,
                        ),
                        loading = false,
                    ),
                    dispatch = intents::add,
                )
            }
        }

        compose.onNodeWithText("Add clue").performScrollTo().performClick()

        assertEquals(listOf(GameDetailIntent.AddClueSelected), intents)
    }

    @Test
    fun clue_authoring_screen_dispatches_back_intent_without_capturing() {
        val intents = mutableListOf<ClueAuthoringIntent>()
        compose.setContent {
            EyespieTheme {
                ClueAuthoringScreen(
                    state = ClueAuthoringState(),
                    dispatch = intents::add,
                )
            }
        }

        compose.onNodeWithText("Back to game").performScrollTo().performClick()

        assertEquals(listOf(ClueAuthoringIntent.Back), intents)
    }

    @Test
    fun clue_authoring_screen_retake_returns_to_live_camera_without_submitting() {
        val intents = mutableListOf<ClueAuthoringIntent>()
        compose.setContent {
            EyespieTheme {
                FakeCameraCaptureSurface {
                    ClueAuthoringScreen(
                        state = ClueAuthoringState(
                            clue = "Find the lighthouse",
                            expectedAnswer = "Lighthouse",
                        ),
                        dispatch = intents::add,
                    )
                }
            }
        }

        compose.onNodeWithText("Expected answer (creator-only)").assertDoesNotExist()
        compose.onNodeWithContentDescription("Capture clue target").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Expected answer (creator-only)").assertExists()

        compose.onNodeWithText("Retake target").performScrollTo().performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Expected answer (creator-only)").assertDoesNotExist()
        compose.onNodeWithContentDescription("Capture clue target").assertExists()
        assertEquals(listOf(ClueAuthoringIntent.DismissFailure), intents)
    }

    @Test
    fun play_screen_dispatches_back_intent_without_resolving_dependencies() {
        val intents = mutableListOf<PlayGameIntent>()
        compose.setContent {
            EyespieTheme {
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

        compose.onNodeWithText("Back to game").performClick()

        assertEquals(listOf(PlayGameIntent.Back), intents)
    }

    @Test
    fun utility_screen_reopens_onboarding_after_scrolling_long_copy() {
        val intents = mutableListOf<UtilityIntent>()
        compose.setContent {
            EyespieTheme {
                UtilityScreen(
                    state = UtilityState(
                        content = UtilityContent("Agent", "player-1"),
                        loading = false,
                    ),
                    dispatch = intents::add,
                )
            }
        }

        compose.onNodeWithText("Show how Eyespie works").performScrollTo().performClick()

        assertEquals(listOf(UtilityIntent.OnboardingSelected), intents)
    }
}

@Composable
private fun FakeCameraCaptureSurface(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalCameraCaptureSurfaceOverride provides { modifier, onCaptured, captureOverlay ->
            Box(modifier = modifier) {
                captureOverlay {
                    onCaptured(CapturedImage.fromEncoded(byteArrayOf(1)))
                }
            }
        },
        content = content,
    )
}
