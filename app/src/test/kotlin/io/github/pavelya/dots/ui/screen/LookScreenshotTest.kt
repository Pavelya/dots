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
import io.github.pavelya.dots.data.PieceStyleId
import io.github.pavelya.dots.data.RingEffectId
import io.github.pavelya.dots.data.ThemeId
import io.github.pavelya.dots.ui.theme.DotsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Pieces and Rings pages and their rows in Settings (the wireframes' frames "Pieces", "Rings"
 * and "Settings", D-30), shown next to their frames in the PR. As on the frames, the Pieces page
 * has Hearts and clubs in use and the Rings page Standard, on a debug build in Notebook. Reference
 * images in app/src/test/screenshots; they are recorded in CI (CLAUDE.md, "Recording screenshots").
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class LookScreenshotTest {
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

    /** The Pieces page on a debug build, which offers every piece style. */
    private fun piecesPage(theme: ThemeId = ThemeId.NOTEBOOK, inUse: PieceStyleId = PieceStyleId.HEARTS_AND_CLUBS, dark: Boolean = false) = show(dark) {
        PiecesScreen(theme, PieceStyleId.entries, inUse, onUse = {}, debugNote = true, onShop = null, onBack = {})
    }

    /** The Rings page on a debug build, which offers every ring effect. */
    private fun ringsPage(theme: ThemeId = ThemeId.NOTEBOOK, inUse: RingEffectId = RingEffectId.STANDARD, dark: Boolean = false) = show(dark) {
        RingsScreen(theme, RingEffectId.entries, inUse, onUse = {}, debugNote = true, onShop = null, onBack = {})
    }

    @Test
    fun `pieces page`() {
        piecesPage()
        capture("pieces_page")
    }

    @Test
    fun `pieces page in dark mode`() {
        piecesPage(dark = true)
        capture("pieces_page_dark")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun `pieces page on a small phone`() {
        piecesPage()
        capture("pieces_page_small_phone")
    }

    @Test
    fun `pieces page in Star chart`() {
        piecesPage(ThemeId.STAR_CHART)
        capture("pieces_page_star_chart")
    }

    @Test
    fun `pieces page in Neon arcade, dark`() {
        piecesPage(ThemeId.NEON_ARCADE, dark = true)
        capture("pieces_page_neon_arcade_dark")
    }

    /** Once the Shop exists: a player who owns Hearts and clubs and has it in use, with the link to the Shop. */
    @Test
    fun `pieces page after the Shop`() {
        show {
            PiecesScreen(ThemeId.NOTEBOOK, listOf(PieceStyleId.STANDARD, PieceStyleId.HEARTS_AND_CLUBS), PieceStyleId.HEARTS_AND_CLUBS, onUse = {}, debugNote = false, onShop = {}, onBack = {})
        }
        capture("pieces_page_after_shop")
    }

    @Test
    fun `rings page`() {
        ringsPage()
        capture("rings_page")
    }

    @Test
    fun `rings page in dark mode`() {
        ringsPage(dark = true)
        capture("rings_page_dark")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun `rings page on a small phone`() {
        ringsPage()
        capture("rings_page_small_phone")
    }

    @Test
    fun `rings page in Neon arcade`() {
        ringsPage(ThemeId.NEON_ARCADE, inUse = RingEffectId.LASSO)
        capture("rings_page_neon_arcade")
    }

    @Test
    fun `rings page in Star chart, dark`() {
        ringsPage(ThemeId.STAR_CHART, dark = true)
        capture("rings_page_star_chart_dark")
    }

    /** Once the Shop exists: a player who owns Rubber stamp, with Standard in use and the link to the Shop. */
    @Test
    fun `rings page after the Shop`() {
        show {
            RingsScreen(ThemeId.NOTEBOOK, listOf(RingEffectId.STANDARD, RingEffectId.RUBBER_STAMP), RingEffectId.STANDARD, onUse = {}, debugNote = false, onShop = {}, onBack = {})
        }
        capture("rings_page_after_shop")
    }

    private fun settingsWithRows(dark: Boolean = false) = show(dark) {
        SettingsScreen(
            AppSettings(), onChange = {}, version = "1.0", onPage = {}, onDeveloperMenu = {}, onReplayTutorial = {}, onFeedback = {}, onBack = {},
            theme = ThemeId.NOTEBOOK, pieces = PieceStyleId.HEARTS_AND_CLUBS, rings = RingEffectId.STANDARD,
        )
    }

    @Test
    fun `settings with the theme, pieces and rings rows`() {
        settingsWithRows()
        capture("settings_look_rows")
    }

    @Test
    fun `settings with the theme, pieces and rings rows in dark mode`() {
        settingsWithRows(dark = true)
        capture("settings_look_rows_dark")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun `settings with the theme, pieces and rings rows on a small phone`() {
        settingsWithRows()
        capture("settings_look_rows_small_phone")
    }
}
