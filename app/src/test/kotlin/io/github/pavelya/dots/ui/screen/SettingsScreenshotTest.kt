package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.ui.theme.DotsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Settings and its pages (the wireframes' frames "Settings" and "Settings details"), shown next to
 * their frames in the PR. Reference images in app/src/test/screenshots; they are recorded in CI
 * (CLAUDE.md, "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class SettingsScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /** Draws [content] as the phone would: 24 dp of status bar at the top, 12 dp of gesture bar at the bottom. */
    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            DotsTheme(dark = dark) {
                Surface { Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) { content() } }
            }
        }
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")

    private fun showSettings(dark: Boolean = false) = show(dark) {
        SettingsScreen(AppSettings(), onChange = {}, version = "1.0", onPage = {}, onDeveloperMenu = {}, onReplayTutorial = {}, onFeedback = {}, onBack = {})
    }

    @Test
    fun settings() {
        showSettings()
        capture("settings_screen")
    }

    @Test
    fun `settings scrolled to the end`() {
        showSettings()
        compose.onNodeWithText("Version 1.0").performScrollTo()
        capture("settings_screen_end")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun `settings on a small phone`() {
        showSettings()
        capture("settings_small_phone")
    }

    @Test
    fun `settings in dark mode`() {
        showSettings(dark = true)
        capture("settings_dark")
    }

    @Test
    fun credits() {
        show { SettingsDetailsScreen(SettingsPage.CREDITS, onPage = {}, onBack = {}) }
        capture("settings_credits")
    }

    @Test
    fun licences() {
        show { SettingsDetailsScreen(SettingsPage.LICENCES, onPage = {}, onBack = {}) }
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasText("Apache License Version 2.0", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        capture("settings_licences")
    }

    @Test
    fun `licences scrolled to the end`() {
        show { SettingsDetailsScreen(SettingsPage.LICENCES, onPage = {}, onBack = {}) }
        val playTerms = hasText("Redistributable Code", substring = true)
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(playTerms).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(playTerms).performScrollTo()
        capture("settings_licences_end")
    }

    /** The wireframes' Settings details: Privacy (D-28): Show ad offers, on by default. */
    @Test
    fun privacy() {
        val changes = mutableListOf<Boolean>()
        show { SettingsDetailsScreen(SettingsPage.PRIVACY, onPage = {}, onBack = {}, onShowAdOffers = { changes += it }) }
        compose.onNodeWithText("Ad choices").assertDoesNotExist()
        compose.onNode(hasText("Show ad offers") and isToggleable()).assertIsOn()
        capture("settings_privacy")

        compose.onNodeWithText("Show ad offers").performClick()
        assertEquals(listOf(false), changes)
    }

    /** Where the law gives the choice (the EU, the UK): Ad choices opens Google's form again. */
    @Test
    fun `privacy with Ad choices`() {
        var opened = 0
        show { SettingsDetailsScreen(SettingsPage.PRIVACY, onPage = {}, onBack = {}, adChoices = true, onAdChoices = { opened++ }, showAdOffers = false) }
        compose.onNode(hasText("Show ad offers") and isToggleable()).assertIsOff()
        capture("settings_privacy_ad_choices")

        compose.onNodeWithText("Ad choices").performClick()
        assertEquals(1, opened)
    }
}
