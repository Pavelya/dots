package io.github.pavelya.dots.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.Credits
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.ui.theme.DotsTheme
import org.junit.Assert.assertEquals
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

    /** The settings the screen last saved. */
    private var latest = AppSettings()
    private val pages = mutableListOf<SettingsPage>()
    private var developerMenus = 0
    private var replays = 0

    /** Settings over [start], saving each change as the app does: applied to the latest settings. */
    private fun settings(start: AppSettings = AppSettings()) {
        latest = start
        compose.setContent {
            var settings by remember { mutableStateOf(start) }
            SettingsScreen(
                settings,
                onChange = { change -> settings = change(settings); latest = settings },
                version = "1.0",
                onPage = { pages += it },
                onDeveloperMenu = { developerMenus++ },
                onReplayTutorial = { replays++ },
                onBack = {},
            )
        }
    }

    @Test
    fun `the volumes change music and sounds, each on its own`() {
        settings()
        compose.onNodeWithContentDescription("Music").performSemanticsAction(SemanticsActions.SetProgress) { it(30f) }
        compose.waitForIdle()
        assertEquals(30, latest.musicVolume)
        assertEquals(AppSettings.DEFAULT_VOLUME, latest.effectsVolume)

        compose.onNodeWithContentDescription("Sounds").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        compose.waitForIdle()
        assertEquals(0, latest.effectsVolume)
        assertEquals(30, latest.musicVolume)
    }

    @Test
    fun `each switch turns its setting on and off`() {
        settings()
        val switches = listOf<Pair<String, (AppSettings) -> Boolean>>(
            "Vibration" to { s: AppSettings -> s.vibration },
            "High-contrast pieces" to { s: AppSettings -> s.highContrast },
            "Threat highlights" to { s: AppSettings -> s.threatHighlight },
        )
        for ((label, value) in switches) {
            val before = value(latest)
            val row = compose.onNodeWithText(label).performScrollTo()
            if (before) row.assertIsOn() else row.assertIsOff()

            row.performClick()
            assertEquals(label, !before, value(latest))
            if (before) row.assertIsOff() else row.assertIsOn()

            row.performClick()
            assertEquals(label, before, value(latest))
        }
        assertEquals(AppSettings(), latest)
    }

    @Test
    fun `vibration starts on, and high contrast and threats start off`() {
        settings()
        compose.onNodeWithText("Vibration").assertIsOn()
        compose.onNodeWithText("High-contrast pieces").assertIsOff()
        compose.onNodeWithText("Threat highlights").assertIsOff()
    }

    @Test
    fun `placing a dot starts as two taps, and drag can be chosen and changed back`() {
        settings()
        compose.onNodeWithText("Two taps").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Drag").assertIsNotSelected()

        compose.onNodeWithText("Drag").performClick()
        assertEquals(PlacingMethod.DRAG, latest.placing)
        compose.onNodeWithText("Drag").assertIsSelected()
        compose.onNodeWithText("Two taps").assertIsNotSelected()

        compose.onNodeWithText("Two taps").performClick()
        assertEquals(PlacingMethod.TWO_TAPS, latest.placing)
        compose.onNodeWithText("Two taps").assertIsSelected()
    }

    @Test
    fun `the screen shows the saved settings`() {
        settings(AppSettings(vibration = false, highContrast = true, threatHighlight = true, placing = PlacingMethod.DRAG, musicVolume = 10, effectsVolume = 90))
        compose.onNodeWithText("Vibration").assertIsOff()
        compose.onNodeWithText("High-contrast pieces").assertIsOn()
        compose.onNodeWithText("Threat highlights").assertIsOn()
        compose.onNodeWithText("Drag").performScrollTo().assertIsSelected()
        compose.onNodeWithContentDescription("Music").assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(10f, 0f..100f)))
        compose.onNodeWithContentDescription("Sounds").assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(90f, 0f..100f)))
    }

    @Test
    fun `seven taps on the version open the developer menu, fewer do not`() {
        settings()
        val version = compose.onNodeWithText("Version 1.0").performScrollTo()
        repeat(DEVELOPER_TAPS - 1) { version.performClick() }
        assertEquals(0, developerMenus)

        version.performClick()
        assertEquals(1, developerMenus)
    }

    @Test
    fun `Help replays the tutorial and opens each of its pages`() {
        settings()
        compose.onNodeWithText("Help").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Replay the tutorial").performScrollTo().performClick()
        assertEquals(1, replays)

        for (row in listOf("Send feedback", "Privacy", "Play Games backup", "Credits and licences")) {
            compose.onNodeWithText(row).performScrollTo().performClick()
        }
        assertEquals(listOf(SettingsPage.FEEDBACK, SettingsPage.PRIVACY, SettingsPage.BACKUP, SettingsPage.CREDITS), pages)
        // Language joins Help once a second language ships (A-10).
        compose.onNodeWithText("Language").assertDoesNotExist()
    }

    @Test
    fun `Credits names the maker and opens the open-source licences`() {
        compose.setContent { SettingsDetailsScreen(SettingsPage.CREDITS, onPage = { pages += it }, onBack = {}) }
        compose.onNodeWithText("Made by").assertIsDisplayed()
        compose.onNodeWithText("School 25").assertIsDisplayed()

        compose.onNodeWithText("Open-source licences").performClick()
        assertEquals(listOf(SettingsPage.LICENCES), pages)
    }

    @Test
    fun `the licences page lists every library and prints each licence in full`() {
        compose.setContent { SettingsDetailsScreen(SettingsPage.LICENCES, onPage = {}, onBack = {}) }
        for (library in Credits.libraries) compose.onNodeWithText(library.name).performScrollTo().assertIsDisplayed()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasText("END OF TERMS AND CONDITIONS", substring = true)).assertExists()
    }

    @Test
    fun `How to play shows the five rules with their pictures, and Play the tutorial`() {
        var played = 0
        compose.setContent { DotsTheme { RulesScreen(onBack = {}, onPlayTutorial = { played++ }) } }

        for (title in listOf("Placing", "Capturing", "Empty rings", "The border", "How a game ends")) {
            compose.onNodeWithText(title).assertExists()
        }
        compose.onNodeWithContentDescription("A red ring of four dots has captured a blue dot.").assertExists()
        compose.onNodeWithContentDescription("A red ring of four dots around an empty point.").assertExists()
        compose.onNodeWithContentDescription("A blue dot on the edge, with red dots around it, is safe.").assertExists()

        compose.onNodeWithText("Play the tutorial").assertIsDisplayed().performClick()
        assertEquals(1, played)
    }
}
