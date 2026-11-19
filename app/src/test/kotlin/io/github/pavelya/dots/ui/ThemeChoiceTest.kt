package io.github.pavelya.dots.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import dots.campaign.Progress
import io.github.pavelya.dots.DotsApplication
import io.github.pavelya.dots.ReviewPrompt
import io.github.pavelya.dots.audio.Audio
import io.github.pavelya.dots.audio.Effect
import io.github.pavelya.dots.audio.Sounding
import io.github.pavelya.dots.audio.Track
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.ThemeId
import io.github.pavelya.dots.ui.board.BoardGeometry
import io.github.pavelya.dots.ui.screen.BOARD_TAG
import io.github.pavelya.dots.ui.theme.GameTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference

/**
 * Choosing a theme on the whole app (docs/design/flows.md, "Choosing a theme"): Settings opens the
 * Theme page on a debug build, the choice is kept, and the game screen is drawn and sounds in the
 * theme in use. The game screen's sounds come from the theme it is drawn in ([GameTheme.sound]),
 * the same theme its board, paper and faces take, so the placing sound tells which theme it is.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h900dp")
class ThemeChoiceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = RuntimeEnvironment.getApplication() as DotsApplication
    private lateinit var nav: NavHostController
    private var opened by mutableIntStateOf(0)

    /** The effects played, with the file heard. */
    private class Recorder : Audio {
        val played: MutableList<Pair<Effect, String>> = Collections.synchronizedList(ArrayList())

        override fun play(effect: Effect, rate: Float, file: String): Sounding {
            played += effect to file
            return Sounding.NONE
        }

        override fun music(track: Track?) = Unit
        override fun settings(sound: Boolean, musicVolume: Int, effectsVolume: Int) = Unit
        override fun onScreen(shown: Boolean) = Unit
    }

    private val audio = Recorder()

    @Before
    fun `a quiet app on the map`() {
        app.reviews = ReviewPrompt { }
        app.audio = audio
        // Level 1 passed, so the app opens on the map. No screen is up yet, so nothing waits on the main thread.
        runBlocking { app.progress.replace(Progress(mapOf("1-1" to 1))) }
    }

    /** Runs [work] on the stores off the main thread, which keeps running (DataStore saves wait for it). */
    private fun save(work: suspend () -> Unit) {
        val job = CoroutineScope(Dispatchers.IO).launch { work() }
        compose.waitUntil(timeoutMillis = 10_000) { job.isCompleted }
    }

    @After
    fun `back to Notebook`() {
        app.debugBuild = true
        save {
            app.themes.use(ThemeId.NOTEBOOK)
            app.settings.update { AppSettings() }
        }
    }

    private fun launch() {
        compose.setContent {
            key(opened) {
                val controller = rememberNavController()
                nav = controller
                DotsApp(controller)
            }
        }
        at(Routes.MAP)
    }

    /** Closes the app and opens it again; what was saved stays. */
    private fun reopen() {
        compose.runOnIdle { opened++ }
        compose.waitForIdle()
        at(Routes.MAP)
    }

    private fun at(route: String) {
        compose.waitUntil(timeoutMillis = 5_000) { nav.currentDestination?.route == route }
        compose.waitForIdle()
    }

    private fun waitFor(matcher: SemanticsMatcher) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private fun tap(text: String) = compose.onNode(hasText(text) or hasContentDescription(text)).performClick()

    private fun back(to: String) {
        Espresso.pressBack()
        at(to)
    }

    /** The card of the theme named [name] on the Theme page. */
    private fun card(name: String) = hasText(name) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)

    /** The theme the store says is in use, read off the main thread. */
    private fun stored(): ThemeId {
        val theme = AtomicReference<ThemeId>()
        save { theme.set(app.themes.inUse.first()) }
        return theme.get()
    }

    /**
     * Opens a two-player game from the map, places a dot at ([x], 0) and returns the file its
     * placing sound played, then goes back to the map.
     */
    private fun placingSound(x: Int): String {
        tap("Play a game")
        tap("Two players")
        at(Routes.GAME)
        waitFor(hasContentDescription("to move", substring = true))
        audio.played.clear()
        compose.onNodeWithTag(BOARD_TAG).performTouchInput {
            val board = BoardGeometry(15, 15, width.toFloat(), height.toFloat())
            down(board.center(x, 2))
            moveTo(board.center(x, 0))
            up()
        }
        compose.waitUntil(timeoutMillis = 5_000) { audio.played.any { it.first in Effect.PLACE } }
        val file = audio.played.first { it.first in Effect.PLACE }.second
        compose.onNodeWithContentDescription("Pause").performClick()
        tap("To the map")
        at(Routes.MAP)
        return file
    }

    @Test
    fun `the game screen is drawn in Notebook until the player picks another theme`() {
        launch()
        assertEquals(ThemeId.NOTEBOOK, stored())
        assertEquals(true, placingSound(0).startsWith("sfx_place_"))
    }

    @Test
    fun `Settings opens the Theme page, a tap puts Star chart in use, and the game screen takes it, also after a restart`() {
        launch()
        tap("Settings")
        at(Routes.SETTINGS)
        waitFor(hasText("Theme") and hasText("Notebook"))
        tap("Theme")
        at(Routes.THEME)
        // Watched off the main thread: DataStore runs each save's change on the main thread.
        val inUse = AtomicReference<ThemeId>()
        val watching = CoroutineScope(Dispatchers.IO).launch { app.themes.inUse.collect { inUse.set(it) } }
        audio.played.clear()
        compose.onNode(card("Notebook")).performClick()
        compose.onNode(card("Star chart")).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { inUse.get() == ThemeId.STAR_CHART }
        watching.cancel()
        assertEquals(listOf(Effect.PLACE_1 to "sfx_star_chart_place_1"), audio.played.toList())

        back(Routes.SETTINGS)
        waitFor(hasText("Theme") and hasText("Star chart"))
        back(Routes.MAP)
        assertEquals(true, placingSound(0).startsWith("sfx_star_chart_place_"))

        reopen()
        assertEquals(ThemeId.STAR_CHART, stored())
        assertEquals(true, placingSound(2).startsWith("sfx_star_chart_place_"))
    }

    @Test
    fun `each theme applies to the game screen`() {
        launch()
        ThemeId.entries.forEachIndexed { i, theme ->
            save { app.themes.use(theme) }
            val file = placingSound(2 * i)
            // sfx_place_2 for Notebook, sfx_star_chart_place_2 for Star chart: the theme's own takes.
            assertEquals(theme.toString(), GameTheme.of(theme).sound(Effect.PLACE_1).substringBeforeLast('_'), file.substringBeforeLast('_'))
        }
    }

    @Test
    fun `a release build has no Theme row, and a debug build's choice goes back to Notebook`() {
        // Saved on a debug build, then opened as a release build.
        runBlocking { app.themes.use(ThemeId.NEON_ARCADE) }
        app.debugBuild = false
        launch()
        tap("Settings")
        at(Routes.SETTINGS)
        waitFor(hasText("The board"))
        compose.onNode(hasText("Theme")).assertDoesNotExist()
        back(Routes.MAP)
        assertEquals(ThemeId.NOTEBOOK, stored())
        assertEquals(true, placingSound(0).startsWith("sfx_place_"))
    }
}
