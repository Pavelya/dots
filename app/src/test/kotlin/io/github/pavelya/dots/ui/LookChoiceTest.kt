package io.github.pavelya.dots.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
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
import io.github.pavelya.dots.data.PieceStyleId
import io.github.pavelya.dots.data.RingEffectId
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
import java.util.concurrent.atomic.AtomicReference

/**
 * Choosing pieces and rings on the whole app (docs/design/flows.md, "Choosing pieces and rings"):
 * on a debug build Settings opens the Pieces and Rings pages, a tap puts an item in use, and the
 * choice is kept across a restart in the value the game screen draws with (V-28). A release build
 * before the Shop has neither row.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h900dp")
class LookChoiceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = RuntimeEnvironment.getApplication() as DotsApplication
    private lateinit var nav: NavHostController
    private var opened by mutableIntStateOf(0)

    private object Quiet : Audio {
        override fun play(effect: Effect, rate: Float, file: String, share: Float) = Sounding.NONE
        override fun music(track: Track?) = Unit
        override fun settings(sound: Boolean, musicVolume: Int, effectsVolume: Int) = Unit
        override fun onScreen(shown: Boolean) = Unit
    }

    @Before
    fun `a quiet app on the map`() {
        app.reviews = ReviewPrompt { }
        app.audio = Quiet
        // Level 1 passed, so the app opens on the map. No screen is up yet, so nothing waits on the main thread.
        runBlocking { app.progress.replace(Progress(mapOf("1-1" to 1))) }
    }

    /** Runs [work] on the stores off the main thread, which keeps running (DataStore saves wait for it). */
    private fun save(work: suspend () -> Unit) {
        val job = CoroutineScope(Dispatchers.IO).launch { work() }
        compose.waitUntil(timeoutMillis = 10_000) { job.isCompleted }
    }

    @After
    fun `back to Standard`() {
        app.debugBuild = true
        save {
            app.themes.use(PieceStyleId.STANDARD)
            app.themes.use(RingEffectId.STANDARD)
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

    /** The Settings row named [name] with [item] in use on its right. */
    private fun row(name: String, item: String) = hasText(name) and hasText(item) and hasClickAction()

    /** The card named [name] on a Pieces or Rings page. */
    private fun card(name: String) = hasText(name) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)

    private fun settings() {
        tap("Settings")
        at(Routes.SETTINGS)
        waitFor(hasText("The board"))
    }

    /** The piece style and ring effect the store says are in use, read off the main thread. */
    private fun stored(): Pair<PieceStyleId, RingEffectId> {
        val looks = AtomicReference<Pair<PieceStyleId, RingEffectId>>()
        save { looks.set(app.themes.pieces.first() to app.themes.rings.first()) }
        return looks.get()
    }

    @Test
    fun `a tap on the Pieces page puts Hearts and clubs in use, and it survives a restart`() {
        launch()
        settings()
        waitFor(row("Pieces", "Standard"))
        compose.onNode(row("Pieces", "Standard")).performClick()
        at(Routes.PIECES)
        // Watched off the main thread: DataStore runs each save's change on the main thread.
        val inUse = AtomicReference<PieceStyleId>()
        val watching = CoroutineScope(Dispatchers.IO).launch { app.themes.pieces.collect { inUse.set(it) } }
        compose.onNode(card("Hearts and clubs")).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { inUse.get() == PieceStyleId.HEARTS_AND_CLUBS }
        watching.cancel()

        back(Routes.SETTINGS)
        waitFor(row("Pieces", "Hearts and clubs"))
        back(Routes.MAP)

        reopen()
        assertEquals(PieceStyleId.HEARTS_AND_CLUBS to RingEffectId.STANDARD, stored())
        settings()
        waitFor(row("Pieces", "Hearts and clubs"))
        compose.onNode(row("Pieces", "Hearts and clubs")).performClick()
        at(Routes.PIECES)
        waitFor(card("Hearts and clubs") and SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }

    @Test
    fun `a tap on the Rings page puts Rubber stamp in use, and it survives a restart`() {
        launch()
        settings()
        waitFor(row("Rings", "Standard"))
        compose.onNode(row("Rings", "Standard")).performClick()
        at(Routes.RINGS)
        val inUse = AtomicReference<RingEffectId>()
        val watching = CoroutineScope(Dispatchers.IO).launch { app.themes.rings.collect { inUse.set(it) } }
        compose.onNode(card("Rubber stamp")).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { inUse.get() == RingEffectId.RUBBER_STAMP }
        watching.cancel()

        back(Routes.SETTINGS)
        waitFor(row("Rings", "Rubber stamp"))
        back(Routes.MAP)

        reopen()
        assertEquals(PieceStyleId.STANDARD to RingEffectId.RUBBER_STAMP, stored())
        settings()
        waitFor(row("Rings", "Rubber stamp"))
    }

    @Test
    fun `a release build has no Pieces or Rings row, and a debug build's choice goes back to Standard`() {
        // Saved on a debug build, then opened as a release build.
        runBlocking {
            app.themes.use(PieceStyleId.FLAME_AND_FROST)
            app.themes.use(RingEffectId.LASSO)
        }
        app.debugBuild = false
        launch()
        settings()
        compose.onNode(hasText("Pieces")).assertDoesNotExist()
        compose.onNode(hasText("Rings")).assertDoesNotExist()
        back(Routes.MAP)
        assertEquals(PieceStyleId.STANDARD to RingEffectId.STANDARD, stored())
    }
}
