package com.micrantha.eyespie.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.presentation.theme.EyespieTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraLayoutShutterTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun shutterAcknowledgesTapBlocksDuplicatesAndResetsAfterCapture() {
        var captureCount = 0
        var finishCapture: ((CapturedImage) -> Unit)? = null

        compose.setContent {
            EyespieTheme {
                CompositionLocalProvider(
                    LocalCameraCaptureSurfaceOverride provides { modifier, onCaptured, captureOverlay ->
                        finishCapture = onCaptured
                        Box(modifier) {
                            captureOverlay { captureCount++ }
                        }
                    },
                ) {
                    CameraLayout(
                        onBack = {},
                        onCaptured = {},
                        onCameraError = {},
                        onAvailabilityChanged = {},
                        busy = false,
                        recoveryMessage = "Camera unavailable",
                        captureButton = { capture ->
                            Button(onClick = capture) { Text("Test shutter") }
                        },
                    )
                }
            }
        }

        compose.onNodeWithText("Test shutter").performClick()
        compose.onNodeWithText("Capturing…").assertIsDisplayed()
        compose.onNodeWithText("Test shutter").performClick()
        compose.runOnIdle { assertEquals(1, captureCount) }

        compose.runOnIdle {
            finishCapture!!.invoke(CapturedImage.fromEncoded(byteArrayOf(1)))
        }
        compose.onNodeWithText("Capturing…").assertDoesNotExist()
        compose.onNodeWithText("Test shutter").performClick()
        compose.runOnIdle { assertEquals(2, captureCount) }
    }
}
