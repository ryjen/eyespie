package com.micrantha.eyespie.core.ui.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.micrantha.eyespie.EyesPieTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-xhdpi")
class AppTitleSnapshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun snapshotAppTitle() {
        composeTestRule.setContent {
            EyesPieTheme {
                AppTitle()
            }
        }

        composeTestRule.onRoot().captureRoboImage()
        error("Controlled CI diagnostics smoke failure")
    }
}
