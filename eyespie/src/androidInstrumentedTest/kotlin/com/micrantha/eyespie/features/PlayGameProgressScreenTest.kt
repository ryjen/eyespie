package com.micrantha.eyespie.features

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.features.play.PlayGameContent
import com.micrantha.eyespie.features.play.PlayGameScreen
import com.micrantha.eyespie.features.play.PlayGameState
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.presentation.LocalCameraCaptureSurfaceOverride
import com.micrantha.eyespie.presentation.theme.EyespieTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayGameProgressScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun play_screen_shows_progress_and_disables_capture_while_matching() {
        compose.setContent {
            EyespieTheme {
                FakeCameraCaptureSurface {
                    PlayGameScreen(
                        state = PlayGameState(
                            gameId = GameId("game-1"),
                            thingId = ThingId("thing-1"),
                            content = PlayGameContent(
                                gameName = "Trip",
                                clueText = "Find the marker",
                                matched = false,
                                bestSimilarity = null,
                            ),
                            loading = false,
                            busy = true,
                        ),
                        dispatch = {},
                    )
                }
            }
        }

        assertTrue(compose.onAllNodesWithText("Checking clue…").fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithContentDescription("Checking clue progress").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithContentDescription("Checking object").assertIsNotEnabled()
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
