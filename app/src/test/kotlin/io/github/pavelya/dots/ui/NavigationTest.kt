package io.github.pavelya.dots.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import dots.engine.GameSettings
import dots.log.GameLogEntry
import dots.log.Outcome
import io.github.pavelya.dots.DotsApplication
import io.github.pavelya.dots.ui.screen.DEVELOPER_TAPS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** The routes and Back of docs/design/flows.md, on the whole app. */
@RunWith(RobolectricTestRunner::class)
class NavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = RuntimeEnvironment.getApplication() as DotsApplication
    private lateinit var nav: NavHostController
    private val visited = mutableSetOf<String>()

    private fun launch() {
        compose.setContent {
            val controller = rememberNavController()
            remember(controller) {
                nav = controller
                controller.addOnDestinationChangedListener { _, destination, _ -> destination.route?.let { visited += it } }
            }
            DotsApp(controller)
        }
        at(Routes.MAP)
    }

    /** Waits until the screen on top is [route]. */
    private fun at(route: String) {
        compose.waitUntil(timeoutMillis = 5_000) { nav.currentDestination?.route == route }
    }

    private fun waitFor(text: String, substring: Boolean = false) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty() }

    private fun gone(text: String) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isEmpty() }

    private fun tap(text: String) = compose.onNodeWithText(text).performClick()

    private fun back() {
        Espresso.pressBack()
        compose.waitForIdle()
    }

    /** Opens [entry] on the current screen, checks it leads to [route], then that Back returns to [returnTo]. */
    private fun visit(entry: String, route: String, returnTo: String = Routes.MAP) {
        tap(entry)
        at(route)
        back()
        at(returnTo)
    }

    private fun openDeveloperMenu() {
        tap("Settings")
        at(Routes.SETTINGS)
        waitFor("Version", substring = true)
        repeat(DEVELOPER_TAPS) { compose.onNode(hasText("Version", substring = true)).performClick() }
        at(Routes.DEVELOPER)
    }

    @Test
    fun `every route opens, and Back leads back to the map`() {
        launch()

        visit("Today’s puzzle", Routes.DAILY)
        visit("Play a game", Routes.PLAY)
        visit("How to play", Routes.RULES)
        visit("Stats", Routes.STATS)
        visit("Shop", Routes.SHOP)
        visit("Settings", Routes.SETTINGS)

        // Level card, then Boosters over it: Back closes Boosters first.
        tap("Level 1")
        at(Routes.LEVEL)
        visit("Boosters", Routes.BOOSTERS, returnTo = Routes.LEVEL)
        back()
        at(Routes.MAP)

        tap("Shop")
        at(Routes.SHOP)
        visit("A sample item", Routes.COSMETIC, returnTo = Routes.SHOP)
        back()
        at(Routes.MAP)

        tap("Settings")
        at(Routes.SETTINGS)
        visit("Credits and licences", Routes.SETTINGS_DETAILS, returnTo = Routes.SETTINGS)
        back()
        at(Routes.MAP)

        openDeveloperMenu()
        back()
        at(Routes.SETTINGS)
        back()
        at(Routes.MAP)

        // The chest opens from the Result of every 10th level, which A-03 and A-04 build.
        compose.runOnUiThread { nav.navigate(Routes.CHEST) }
        at(Routes.CHEST)
        back()
        at(Routes.MAP)

        tap("Play a game")
        tap("Free play")
        at(Routes.GAME)
        waitFor("Your turn · 33 dots left")
        compose.onNodeWithContentDescription("Pause").performClick()
        tap("To the map")
        at(Routes.MAP)

        assertEquals(Routes.ALL.toSet(), visited)
    }

    @Test
    fun `on the board, Back opens Pause, and How to play returns to Pause`() {
        launch()
        tap("Play a game")
        tap("Two players")
        at(Routes.GAME)
        waitFor("Red to move · 33 dots left")

        back()
        waitFor("Resume")
        back()
        gone("Resume")
        assertEquals(Routes.GAME, nav.currentDestination?.route)

        compose.onNodeWithContentDescription("Pause").performClick()
        tap("How to play")
        at(Routes.RULES)
        back()
        at(Routes.GAME)
        waitFor("Resume")
        tap("Resume")
        gone("Resume")

        compose.onNode(hasContentDescription("Pause")).performClick()
        tap("To the map")
        at(Routes.MAP)
    }

    @Test
    fun `Back on the map leaves the app`() {
        launch()

        Espresso.pressBackUnconditionally()

        assertTrue(compose.activity.isFinishing)
    }

    @Test
    fun `a game left for the map is resumed by its card, and the other card starts a new one`() {
        launch()
        tap("Play a game")
        tap("Free play")
        at(Routes.GAME)
        waitFor("Your turn · 33 dots left")
        val first = app.games.current.value
        compose.onNodeWithContentDescription("Pause").performClick()
        tap("To the map")
        at(Routes.MAP)

        tap("Play a game")
        waitFor("Resume your game")
        tap("Free play")
        at(Routes.GAME)
        assertSame(first, app.games.current.value)

        compose.onNodeWithContentDescription("Pause").performClick()
        tap("To the map")
        at(Routes.MAP)
        tap("Play a game")
        tap("Two players")
        waitFor("Red to move · 33 dots left")
        assertNotSame(first, app.games.current.value)
    }

    @Test
    fun `the developer menu holds the test settings and shares the game log`() {
        val finished = GameLogEntry.start(GameSettings.POC, "easy", threatHighlight = false, now = 0).copy(outcome = Outcome.FINISHED)
        app.games.finish(finished)
        launch()
        openDeveloperMenu()
        waitFor("1 game in the log, kept on this phone until you share it. It holds your moves and results, nothing else.")

        compose.onNodeWithText("Share the game log").performScrollTo().assertIsEnabled().performClick()

        compose.waitUntil(timeoutMillis = 5_000) { shadowOf(app).peekNextStartedActivity() != null }
        val chooser = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val send = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        val uri = send.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)!!
        assertEquals("content://io.github.pavelya.pinfold.logs/games/game-log.txt", uri.toString())
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
