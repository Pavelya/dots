package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.ThemeId
import io.github.pavelya.dots.ui.theme.DotsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Theme page and the Theme row in Settings (the wireframes' frames "Theme" and "Settings"),
 * shown next to their frames in the PR. Reference images in app/src/test/screenshots; they are
 * recorded in CI (CLAUDE.md, "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class ThemeScreenshotTest {
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

    /** The page on a debug build, which offers every theme, with [inUse] ticked. */
    private fun debugPage(inUse: ThemeId = ThemeId.NOTEBOOK, dark: Boolean = false) = show(dark) {
        ThemeScreen(ThemeId.entries, inUse, onUse = {}, debugNote = true, onShop = null, onBack = {})
    }

    @Test
    fun `theme page`() {
        debugPage()
        capture("theme_page")
    }

    @Test
    fun `theme page in dark mode`() {
        debugPage(dark = true)
        capture("theme_page_dark")
    }

    @Test
    fun `theme page with Neon arcade in use`() {
        debugPage(ThemeId.NEON_ARCADE)
        capture("theme_page_neon_arcade_in_use")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun `theme page on a small phone`() {
        debugPage()
        capture("theme_page_small_phone")
    }

    /** Once the Shop exists: a player who owns Star chart and has it in use, with the link to the Shop. */
    @Test
    fun `theme page after the Shop`() {
        show {
            ThemeScreen(listOf(ThemeId.NOTEBOOK, ThemeId.STAR_CHART), ThemeId.STAR_CHART, onUse = {}, debugNote = false, onShop = {}, onBack = {})
        }
        capture("theme_page_after_shop")
    }

    @Test
    fun `settings with the theme row`() {
        show {
            SettingsScreen(AppSettings(), onChange = {}, version = "1.0", onPage = {}, onDeveloperMenu = {}, onReplayTutorial = {}, onFeedback = {}, onBack = {}, theme = ThemeId.NOTEBOOK)
        }
        capture("settings_theme_row")
    }
}
