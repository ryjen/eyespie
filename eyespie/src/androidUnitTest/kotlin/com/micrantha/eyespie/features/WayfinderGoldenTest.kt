package com.micrantha.eyespie.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.features.gamedetail.GameDetailContent
import com.micrantha.eyespie.features.gamedetail.GameDetailScreen
import com.micrantha.eyespie.features.gamedetail.GameDetailState
import com.micrantha.eyespie.features.gamedetail.GameDetailThing
import com.micrantha.eyespie.features.home.HomeContent
import com.micrantha.eyespie.features.home.HomeGame
import com.micrantha.eyespie.features.home.HomeImportPreview
import com.micrantha.eyespie.features.home.HomeScreen
import com.micrantha.eyespie.features.home.HomeState
import com.micrantha.eyespie.features.home.HomeThing
import com.micrantha.eyespie.features.onboarding.OnboardingPage
import com.micrantha.eyespie.features.onboarding.OnboardingScreen
import com.micrantha.eyespie.features.onboarding.OnboardingState
import com.micrantha.eyespie.features.play.PlayGameContent
import com.micrantha.eyespie.features.play.PlayGameScreen
import com.micrantha.eyespie.features.play.PlayGameState
import com.micrantha.eyespie.features.utility.UtilityContent
import com.micrantha.eyespie.features.utility.UtilityScreen
import com.micrantha.eyespie.features.utility.UtilityState
import com.micrantha.eyespie.presentation.LocalCameraCaptureSurfaceOverride
import com.micrantha.eyespie.presentation.theme.EyespieTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Deterministic visual references for the canonical Wayfinder alpha surface.
 *
 * The test owns only presentation fixtures: no wall clock, files, keychain, network, camera,
 * MediaPipe model, or application persistence is resolved. Camera-backed states use a static
 * field surface through [LocalCameraCaptureSurfaceOverride].
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = "en-rUS-" + RobolectricDeviceQualifiers.Pixel5,
    fontScale = 1.0f,
)
class WayfinderGoldenTest {
    @get:Rule(order = 0)
    val roborazzi = RoborazziRule(
        options = RoborazziRule.Options(
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(
                    changeThreshold = 0.002f,
                ),
            ),
        ),
    )

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun home_empty() = capture("home_empty") {
        HomeScreen(
            state = HomeState(
                content = HomeContent(
                    identityDisplayName = "Agent Rowan",
                    identityIdSuffix = "9f21b73c4d5e",
                    games = emptyList(),
                ),
                loading = false,
            ),
            dispatch = {},
        )
    }

    @Test
    fun home_populated() = capture("home_populated") {
        HomeScreen(
            state = HomeState(
                content = HomeContent(
                    identityDisplayName = "Agent Rowan",
                    identityIdSuffix = "9f21b73c4d5e",
                    games = listOf(
                        HomeGame(
                            id = GameId("stanley-park-case"),
                            name = "Stanley Park Field Case",
                            things = listOf(
                                HomeThing(
                                    id = ThingId("totem-pole"),
                                    clueText = "Find the watchful face carved in cedar",
                                    matched = true,
                                    bestSimilarity = 0.91,
                                ),
                                HomeThing(
                                    id = ThingId("seawall-marker"),
                                    clueText = "Find the marker beside the seawall",
                                    matched = false,
                                    bestSimilarity = null,
                                ),
                            ),
                            localCreator = true,
                        ),
                        HomeGame(
                            id = GameId("granville-night-run"),
                            name = "Granville Night Run",
                            things = listOf(
                                HomeThing(
                                    id = ThingId("market-sign"),
                                    clueText = "Find the red market sign",
                                    matched = false,
                                    bestSimilarity = 0.42,
                                ),
                            ),
                            localCreator = false,
                        ),
                    ),
                ),
                loading = false,
            ),
            dispatch = {},
        )
    }

    @Test
    fun verified_import_preview() = capture("verified_import_preview") {
        HomeScreen(
            state = HomeState(
                content = HomeContent(
                    identityDisplayName = "Agent Rowan",
                    identityIdSuffix = "9f21b73c4d5e",
                    games = emptyList(),
                ),
                loading = false,
                importPreview = HomeImportPreview(
                    gameName = "Gastown Signal Trail",
                    clueCount = 4,
                    creatorIdSuffix = "31c19ab71d2e",
                    gameIdSuffix = "7c4e92d116a0",
                ),
            ),
            dispatch = {},
        )
    }

    @Test
    fun onboarding_local() = capture("onboarding_local") {
        OnboardingScreen(
            state = OnboardingState(page = OnboardingPage.Local),
            dispatch = {},
        )
    }

    @Test
    fun game_detail_creator() = capture("game_detail_creator") {
        GameDetailScreen(
            state = GameDetailState(
                content = GameDetailContent(
                    name = "Stanley Park Field Case",
                    localCreator = true,
                    things = listOf(
                        GameDetailThing(
                            id = ThingId("totem-pole"),
                            clueText = "Find the watchful face carved in cedar",
                            matched = true,
                            bestSimilarity = 0.91,
                        ),
                        GameDetailThing(
                            id = ThingId("seawall-marker"),
                            clueText = "Find the marker beside the seawall",
                            matched = false,
                            bestSimilarity = null,
                        ),
                        GameDetailThing(
                            id = ThingId("lighthouse"),
                            clueText = "Find the light that guards the harbour",
                            matched = false,
                            bestSimilarity = null,
                        ),
                    ),
                ),
                loading = false,
            ),
            dispatch = {},
        )
    }

    @Test
    fun play_searching() = captureCamera("play_searching") {
        PlayGameScreen(
            state = PlayGameState(
                gameId = GameId("stanley-park-case"),
                thingId = ThingId("seawall-marker"),
                content = PlayGameContent(
                    gameName = "Stanley Park Field Case",
                    clueText = "Find the marker beside the seawall",
                    matched = false,
                    bestSimilarity = null,
                    clueNumber = 2,
                    clueCount = 3,
                    matchedClueCount = 1,
                    nextThingId = ThingId("lighthouse"),
                ),
                loading = false,
            ),
            dispatch = {},
        )
    }

    @Test
    fun play_clue_found() = captureCamera("play_clue_found") {
        PlayGameScreen(
            state = PlayGameState(
                gameId = GameId("stanley-park-case"),
                thingId = ThingId("seawall-marker"),
                content = PlayGameContent(
                    gameName = "Stanley Park Field Case",
                    clueText = "Find the marker beside the seawall",
                    matched = true,
                    bestSimilarity = 0.93,
                    clueNumber = 2,
                    clueCount = 3,
                    matchedClueCount = 2,
                    nextThingId = ThingId("lighthouse"),
                ),
                loading = false,
            ),
            dispatch = {},
        )
    }

    @Test
    fun play_case_complete() = captureCamera("play_case_complete") {
        PlayGameScreen(
            state = PlayGameState(
                gameId = GameId("stanley-park-case"),
                thingId = ThingId("lighthouse"),
                content = PlayGameContent(
                    gameName = "Stanley Park Field Case",
                    clueText = "Find the light that guards the harbour",
                    matched = true,
                    bestSimilarity = 0.95,
                    clueNumber = 3,
                    clueCount = 3,
                    matchedClueCount = 3,
                    nextThingId = null,
                ),
                loading = false,
            ),
            dispatch = {},
        )
    }

    @Test
    fun utility_profile_privacy() = capture("utility_profile_privacy") {
        UtilityScreen(
            state = UtilityState(
                content = UtilityContent(
                    identityDisplayName = "Agent Rowan",
                    identityIdSuffix = "9f21b73c4d5e",
                ),
                loading = false,
            ),
            dispatch = {},
        )
    }

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            EyespieTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    content()
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("$name.png")
    }

    private fun captureCamera(
        name: String,
        content: @Composable () -> Unit,
    ) {
        capture(name) {
            CompositionLocalProvider(
                LocalCameraCaptureSurfaceOverride provides { modifier, _, captureOverlay ->
                    StaticFieldCamera(modifier, captureOverlay)
                },
                content = content,
            )
        }
    }
}

@Composable
private fun StaticFieldCamera(
    modifier: Modifier,
    captureOverlay: @Composable ((capture: () -> Unit) -> Unit),
) {
    Box(
        modifier = modifier.background(Color(0xFF66766D)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(172.dp)
                .border(2.dp, Color(0x99F5F5F0), MaterialTheme.shapes.large),
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Color(0xCCF5F5F0), MaterialTheme.shapes.small),
        )
        captureOverlay({})
    }
}
