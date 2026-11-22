package io.github.pavelya.dots.ui.screen

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import io.github.pavelya.dots.audio.Audio
import io.github.pavelya.dots.audio.Effect
import io.github.pavelya.dots.audio.LocalAudio
import io.github.pavelya.dots.audio.Sounding
import io.github.pavelya.dots.audio.Track
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.ThemeId
import io.github.pavelya.dots.ui.theme.DotsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Theme row in Settings and the Theme page (docs/design/flows.md, "Choosing a theme"). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class ThemeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    /** The effects played, with the file heard. */
    private class Recorder : Audio {
        val played = mutableListOf<Pair<Effect, String>>()

        override fun play(effect: Effect, rate: Float, file: String, share: Float): Sounding {
            played += effect to file
            return Sounding.NONE
        }

        override fun music(track: Track?) = Unit
        override fun settings(sound: Boolean, musicVolume: Int, effectsVolume: Int) = Unit
        override fun onScreen(shown: Boolean) = Unit
    }

    private val audio = Recorder()
    private val used = mutableListOf<ThemeId>()
    private var shop = 0

    /** The Theme page with [choices], the theme in use changing as the app changes it. */
    private fun page(
        choices: List<ThemeId> = ThemeId.entries,
        start: ThemeId = ThemeId.NOTEBOOK,
        debugNote: Boolean = true,
        withShop: Boolean = false,
    ) {
        compose.setContent {
            var inUse by remember { mutableStateOf(start) }
            DotsTheme(dark = false) {
                CompositionLocalProvider(LocalAudio provides audio) {
                    ThemeScreen(
                        choices = choices,
                        inUse = inUse,
                        onUse = { used += it; inUse = it },
                        debugNote = debugNote,
                        onShop = if (withShop) ({ shop++ }) else null,
                        onBack = {},
                    )
                }
            }
        }
    }

    /** The card of the theme named [name]: TalkBack reads its name and line as one. */
    private fun card(name: String) = compose.onNode(hasText(name) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))

    @Test
    fun `every theme has a card in order, with its name and line, and Notebook is in use`() {
        page()
        val names = listOf("Notebook", "Star chart", "Neon arcade")
        val tops = names.map { card(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(tops.sorted(), tops)
        card("Notebook").assertIsSelected()
        card("Star chart").assertIsNotSelected()
        card("Neon arcade").assertIsNotSelected()
        compose.onNodeWithText("Red and blue ink on squared paper.").assertExists()
        compose.onNodeWithText("Gold stars and ice-blue comets on a chart of the sky.").assertExists()
        compose.onNodeWithText("Magenta and cyan pieces, with rings of neon light.").assertExists()
        compose.onNodeWithText("Light or dark follows your phone’s dark theme.").assertExists()
    }

    @Test
    fun `TalkBack reads a card as its name and line, not the board or In use`() {
        page()
        val text = card("Notebook").fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }
        assertEquals(listOf("Notebook", "Red and blue ink on squared paper."), text)
    }

    @Test
    fun `tapping another theme puts it in use at once and plays its first placing sound`() {
        page()
        card("Star chart").performClick()
        assertEquals(listOf(ThemeId.STAR_CHART), used)
        assertEquals(listOf(Effect.PLACE_1 to "sfx_star_chart_place_1"), audio.played)
        card("Star chart").assertIsSelected()
        card("Notebook").assertIsNotSelected()

        card("Notebook").performClick()
        card("Neon arcade").performClick()
        assertEquals(listOf(ThemeId.STAR_CHART, ThemeId.NOTEBOOK, ThemeId.NEON_ARCADE), used)
        assertEquals(listOf("sfx_star_chart_place_1", "sfx_place_1", "sfx_neon_arcade_place_1"), audio.played.map { it.second })
    }

    @Test
    fun `tapping the theme in use does nothing`() {
        page(start = ThemeId.NEON_ARCADE)
        card("Neon arcade").assertIsSelected().performClick()
        assertTrue(used.isEmpty())
        assertTrue(audio.played.isEmpty())
    }

    @Test
    fun `a theme the player can't choose is not on the page`() {
        page(choices = listOf(ThemeId.NOTEBOOK, ThemeId.STAR_CHART), debugNote = false)
        card("Notebook").assertExists()
        card("Star chart").assertExists()
        compose.onNodeWithText("Neon arcade").assertDoesNotExist()
    }

    @Test
    fun `a debug build says every theme is here, and nothing points to the Shop before it exists`() {
        page()
        compose.onNodeWithText("Debug build: every theme is here. Players get only Notebook until the Shop opens.").assertExists()
        compose.onNodeWithText("More themes in the Shop").assertDoesNotExist()
    }

    @Test
    fun `once the Shop exists, a player who lacks a theme gets a link to it`() {
        page(choices = listOf(ThemeId.NOTEBOOK, ThemeId.STAR_CHART), debugNote = false, withShop = true)
        compose.onNodeWithText("Debug build", substring = true).assertDoesNotExist()
        compose.onNodeWithText("More themes in the Shop").performClick()
        assertEquals(1, shop)
    }

    @Test
    fun `a player who owns every theme gets no Shop link`() {
        page(debugNote = false, withShop = true)
        compose.onNodeWithText("More themes in the Shop").assertDoesNotExist()
    }

    private fun settings(theme: ThemeId?, onTheme: () -> Unit = {}) {
        compose.setContent {
            DotsTheme(dark = false) {
                SettingsScreen(
                    AppSettings(),
                    onChange = {},
                    version = "1.0",
                    onPage = {},
                    onDeveloperMenu = {},
                    onReplayTutorial = {},
                    onFeedback = {},
                    onBack = {},
                    theme = theme,
                    onTheme = onTheme,
                )
            }
        }
    }

    @Test
    fun `Settings shows the theme in use and opens the Theme page`() {
        var opened = 0
        settings(ThemeId.STAR_CHART) { opened++ }
        compose.onNode(hasText("Theme") and hasText("Star chart")).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun `Settings has no Theme row when there is one theme to choose`() {
        settings(theme = null)
        compose.onNodeWithText("The board").assertExists()
        compose.onNodeWithText("Theme").assertDoesNotExist()
    }
}
