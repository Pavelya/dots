package io.github.pavelya.dots.ui.screen

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.pavelya.dots.audio.Audio
import io.github.pavelya.dots.audio.Effect
import io.github.pavelya.dots.audio.LocalAudio
import io.github.pavelya.dots.audio.Sounding
import io.github.pavelya.dots.audio.Track
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.PieceStyleId
import io.github.pavelya.dots.data.RingEffectId
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

/**
 * The Pieces and Rings rows in Settings and their pages (docs/design/flows.md, "Choosing pieces
 * and rings"): the words, the order, TalkBack, the sounds, and which items are listed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp-xhdpi")
class LookScreensTest {
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
    private val usedPieces = mutableListOf<PieceStyleId>()
    private val usedRings = mutableListOf<RingEffectId>()
    private var shop = 0

    /** The Pieces page in [theme] with [choices], the style in use changing as the app changes it. */
    private fun piecesPage(
        choices: List<PieceStyleId> = PieceStyleId.entries,
        start: PieceStyleId = PieceStyleId.STANDARD,
        theme: ThemeId = ThemeId.NOTEBOOK,
        debugNote: Boolean = true,
        withShop: Boolean = false,
    ) {
        compose.setContent {
            var inUse by remember { mutableStateOf(start) }
            DotsTheme(dark = false) {
                CompositionLocalProvider(LocalAudio provides audio) {
                    PiecesScreen(theme, choices, inUse, onUse = { usedPieces += it; inUse = it }, debugNote, if (withShop) ({ shop++ }) else null, onBack = {})
                }
            }
        }
    }

    /** The Rings page, the same way. */
    private fun ringsPage(
        choices: List<RingEffectId> = RingEffectId.entries,
        start: RingEffectId = RingEffectId.STANDARD,
        theme: ThemeId = ThemeId.NOTEBOOK,
        debugNote: Boolean = true,
        withShop: Boolean = false,
    ) {
        compose.setContent {
            var inUse by remember { mutableStateOf(start) }
            DotsTheme(dark = false) {
                CompositionLocalProvider(LocalAudio provides audio) {
                    RingsScreen(theme, choices, inUse, onUse = { usedRings += it; inUse = it }, debugNote, if (withShop) ({ shop++ }) else null, onBack = {})
                }
            }
        }
    }

    /** The card named [name]: TalkBack reads its name and line as one. */
    private fun card(name: String) = compose.onNode(hasText(name) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))

    private fun cardText(name: String) = card(name).fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }

    private fun tops(vararg names: String) = names.map { card(it).fetchSemanticsNode().boundsInRoot.top }

    @Test
    fun `the Pieces page lists every piece style in order, Standard first and in use, with D-30's words`() {
        piecesPage()
        compose.onNodeWithText("Pieces").assertExists()
        val tops = tops("Standard", "Hearts and clubs", "Flame and frost")
        assertEquals(tops.sorted(), tops)
        card("Standard").assertIsSelected()
        card("Hearts and clubs").assertIsNotSelected()
        card("Flame and frost").assertIsNotSelected()
        assertEquals(listOf("Standard", "Your theme’s own pieces."), cardText("Standard"))
        assertEquals(listOf("Hearts and clubs", "Hearts for one side, clubs for the other."), cardText("Hearts and clubs"))
        assertEquals(listOf("Flame and frost", "Flames for one side, snowflakes for the other."), cardText("Flame and frost"))
        compose.onNodeWithText("Shown in your theme. Light or dark follows your phone’s dark theme.").assertExists()
        compose.onNodeWithText("Debug build: every piece style is here. Players see only the ones they own.").assertExists()
        compose.onNodeWithText("More piece styles in the Shop").assertDoesNotExist()
    }

    @Test
    fun `the Rings page lists every ring effect in order, with D-30's words`() {
        ringsPage(start = RingEffectId.LASSO)
        compose.onNodeWithText("Rings").assertExists()
        val tops = tops("Standard", "Lasso", "Rubber stamp")
        assertEquals(tops.sorted(), tops)
        card("Lasso").assertIsSelected()
        card("Standard").assertIsNotSelected()
        assertEquals(listOf("Standard", "Your theme’s own ring."), cardText("Standard"))
        assertEquals(listOf("Lasso", "A rope thrown round the area and pulled tight."), cardText("Lasso"))
        assertEquals(listOf("Rubber stamp", "A double line, and the area stamped down at once."), cardText("Rubber stamp"))
        compose.onNodeWithText("Debug build: every ring effect is here. Players see only the ones they own.").assertExists()
    }

    @Test
    fun `the Pieces cards are one group of choices named Pieces`() {
        piecesPage()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Pieces"))).assertExists()
    }

    @Test
    fun `the Rings cards are one group of choices named Rings`() {
        ringsPage()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Rings"))).assertExists()
    }

    @Test
    fun `a piece style's card puts it in use at once with the theme's first placing sound`() {
        piecesPage(theme = ThemeId.STAR_CHART)
        card("Hearts and clubs").performClick()
        assertEquals(listOf(PieceStyleId.HEARTS_AND_CLUBS), usedPieces)
        assertEquals(listOf(Effect.PLACE_1 to "sfx_star_chart_place_1"), audio.played)
        card("Hearts and clubs").assertIsSelected()
        card("Standard").assertIsNotSelected()

        card("Flame and frost").performClick()
        assertEquals(listOf(PieceStyleId.HEARTS_AND_CLUBS, PieceStyleId.FLAME_AND_FROST), usedPieces)
    }

    @Test
    fun `a ring effect's card puts it in use at once with the theme's ring closing sound`() {
        ringsPage(theme = ThemeId.NEON_ARCADE)
        card("Rubber stamp").performClick()
        assertEquals(listOf(RingEffectId.RUBBER_STAMP), usedRings)
        assertEquals(listOf(Effect.RING_CLOSE to "sfx_neon_arcade_ring_close"), audio.played)
        card("Rubber stamp").assertIsSelected()
        card("Standard").assertIsNotSelected()

        card("Lasso").performClick()
        assertEquals(listOf(RingEffectId.RUBBER_STAMP, RingEffectId.LASSO), usedRings)
    }

    @Test
    fun `Notebook's ring closing sound is its own`() {
        ringsPage(theme = ThemeId.NOTEBOOK)
        card("Lasso").performClick()
        assertEquals(listOf(Effect.RING_CLOSE to "sfx_ring_close"), audio.played)
    }

    @Test
    fun `tapping the piece style in use does nothing`() {
        piecesPage(start = PieceStyleId.FLAME_AND_FROST)
        card("Flame and frost").assertIsSelected().performClick()
        assertTrue(usedPieces.isEmpty())
        assertTrue(audio.played.isEmpty())
    }

    @Test
    fun `tapping the ring effect in use does nothing`() {
        ringsPage(start = RingEffectId.STANDARD)
        card("Standard").assertIsSelected().performClick()
        assertTrue(usedRings.isEmpty())
        assertTrue(audio.played.isEmpty())
    }

    @Test
    fun `a piece style the player can't choose is not on the page`() {
        piecesPage(choices = listOf(PieceStyleId.STANDARD, PieceStyleId.HEARTS_AND_CLUBS), debugNote = false)
        card("Standard").assertExists()
        card("Hearts and clubs").assertExists()
        compose.onNodeWithText("Flame and frost").assertDoesNotExist()
        compose.onNodeWithText("Debug build", substring = true).assertDoesNotExist()
    }

    @Test
    fun `a ring effect the player can't choose is not on the page`() {
        ringsPage(choices = listOf(RingEffectId.STANDARD, RingEffectId.RUBBER_STAMP), debugNote = false)
        card("Standard").assertExists()
        card("Rubber stamp").assertExists()
        compose.onNodeWithText("Lasso").assertDoesNotExist()
    }

    @Test
    fun `once the Shop exists, a player who lacks a piece style gets a link to it`() {
        piecesPage(choices = listOf(PieceStyleId.STANDARD, PieceStyleId.HEARTS_AND_CLUBS), debugNote = false, withShop = true)
        compose.onNodeWithText("More piece styles in the Shop").performClick()
        assertEquals(1, shop)
    }

    @Test
    fun `once the Shop exists, a player who lacks a ring effect gets a link to it`() {
        ringsPage(choices = listOf(RingEffectId.STANDARD, RingEffectId.LASSO), debugNote = false, withShop = true)
        compose.onNodeWithText("More ring effects in the Shop").performClick()
        assertEquals(1, shop)
    }

    @Test
    fun `a player who owns every piece style gets no Shop link`() {
        piecesPage(debugNote = false, withShop = true)
        compose.onNodeWithText("More piece styles in the Shop").assertDoesNotExist()
    }

    @Test
    fun `a player who owns every ring effect gets no Shop link`() {
        ringsPage(debugNote = false, withShop = true)
        compose.onNodeWithText("More ring effects in the Shop").assertDoesNotExist()
    }

    private fun settings(pieces: PieceStyleId?, rings: RingEffectId?, onPieces: () -> Unit = {}, onRings: () -> Unit = {}) {
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
                    theme = ThemeId.NOTEBOOK,
                    pieces = pieces,
                    onPieces = onPieces,
                    rings = rings,
                    onRings = onRings,
                )
            }
        }
    }

    @Test
    fun `Settings shows the items in use below Theme, read as name and item, and opens their pages`() {
        var pieces = 0
        var rings = 0
        settings(PieceStyleId.HEARTS_AND_CLUBS, RingEffectId.STANDARD, onPieces = { pieces++ }, onRings = { rings++ })
        val piecesRow = compose.onNode(hasText("Pieces") and hasClickAction())
        val ringsRow = compose.onNode(hasText("Rings") and hasClickAction())
        assertEquals(listOf("Pieces", "Hearts and clubs"), piecesRow.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text })
        assertEquals(listOf("Rings", "Standard"), ringsRow.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text })
        val theme = compose.onNode(hasText("Theme") and hasClickAction()).fetchSemanticsNode().boundsInRoot.top
        val tops = listOf(theme, piecesRow.fetchSemanticsNode().boundsInRoot.top, ringsRow.fetchSemanticsNode().boundsInRoot.top)
        assertEquals(tops.sorted(), tops)
        piecesRow.performClick()
        ringsRow.performClick()
        assertEquals(1, pieces)
        assertEquals(1, rings)
    }

    @Test
    fun `Settings has no Pieces or Rings row when only Standard can be chosen`() {
        settings(pieces = null, rings = null)
        compose.onNodeWithText("Theme").assertExists()
        compose.onNodeWithText("Pieces").assertDoesNotExist()
        compose.onNodeWithText("Rings").assertDoesNotExist()
    }
}
