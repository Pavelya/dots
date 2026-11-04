package io.github.pavelya.dots.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.BoardSize
import io.github.pavelya.dots.ui.theme.DotsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class SettingsAndRulesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `choosing a bigger board updates the dot counts shown`() {
        var latest = AppSettings()
        compose.setContent {
            var settings by remember { mutableStateOf(AppSettings()) }
            SettingsScreen(settings, onChange = { settings = it; latest = it }, onBack = {})
        }
        compose.onNodeWithText("Short: 33 dots each").assertIsDisplayed()

        compose.onNodeWithText("20 × 20").performClick()

        assertEquals(BoardSize.LARGE, latest.boardSize)
        compose.onNodeWithText("Short: 60 dots each").assertIsDisplayed()
        compose.onNodeWithText("Long: 120 dots each").assertIsDisplayed()
    }

    @Test
    fun `the threat switch turns on`() {
        var latest = AppSettings()
        compose.setContent {
            var settings by remember { mutableStateOf(AppSettings()) }
            SettingsScreen(settings, onChange = { settings = it; latest = it }, onBack = {})
        }
        compose.onNodeWithText("Show threats").performClick()

        assertTrue(latest.threatHighlight)
    }

    @Test
    fun `the rules show all three pictures`() {
        compose.setContent { DotsTheme { RulesScreen(onBack = {}) } }

        compose.onNodeWithContentDescription("Red’s dots linked into a chain, some diagonally.").assertIsDisplayed()
        compose.onNodeWithContentDescription("A red ring of four dots has captured a blue dot.").assertIsDisplayed()
        compose.onNodeWithContentDescription("A blue dot on the edge, with red dots around it, is safe.").assertIsDisplayed()
        compose.onRoot().captureRoboImage("src/test/screenshots/rules_screen.png")
    }
}
