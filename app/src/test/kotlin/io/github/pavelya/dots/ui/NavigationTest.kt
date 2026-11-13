package io.github.pavelya.dots.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import dots.campaign.Campaign
import dots.campaign.LevelRecord
import dots.campaign.Progress
import dots.engine.Action
import dots.engine.GameRecord
import dots.engine.GameSettings
import dots.engine.Pos
import dots.log.GameLogEntry
import dots.log.Outcome
import io.github.pavelya.dots.DotsApplication
import java.util.concurrent.atomic.AtomicReference
import io.github.pavelya.dots.data.AppSettings
import io.github.pavelya.dots.data.GameLength
import io.github.pavelya.dots.data.GameMode
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.data.SampleProgress
import io.github.pavelya.dots.game.levelGame
import io.github.pavelya.dots.ui.board.BoardGeometry
import io.github.pavelya.dots.ui.screen.BOARD_TAG
import io.github.pavelya.dots.ui.screen.DEVELOPER_TAPS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

/** The routes and Back of docs/design/flows.md, on the whole app. */
@RunWith(RobolectricTestRunner::class)
class NavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = RuntimeEnvironment.getApplication() as DotsApplication
    private lateinit var nav: NavHostController
    private val visited = mutableSetOf<String>()

    /** How many times the app has been opened; a new number opens it again, with a new back stack. */
    private var opened by mutableIntStateOf(0)

    /** Saves [progress], opens the app as the player does, and waits for the loading screen to hand over to [route]. */
    private fun launch(progress: Progress = LEVEL_1_PASSED, route: String = Routes.MAP) {
        runBlocking { app.progress.replace(progress) }
        compose.setContent {
            key(opened) {
                val controller = rememberNavController()
                DisposableEffect(controller) {
                    nav = controller
                    val listener = NavController.OnDestinationChangedListener { _, destination, _ -> destination.route?.let { visited += it } }
                    controller.addOnDestinationChangedListener(listener)
                    onDispose { controller.removeOnDestinationChangedListener(listener) }
                }
                DotsApp(controller)
            }
        }
        at(route)
    }

    /** Closes the app and opens it again, from the loading screen; the saved game and progress stay. */
    private fun reopen(route: String) {
        compose.runOnIdle {
            visited.clear()
            opened++
        }
        compose.waitForIdle()
        at(route)
    }

    /** The level code of the saved game, if it is a campaign game. */
    private val savedLevel get() = (app.games.saved(GameMode.CAMPAIGN)?.record as? LevelRecord)?.level?.code

    /**
     * Waits until the screen on top is [route] and drawn: a dialog's window open, a screen's
     * transition over. Only then does Back reach it.
     */
    private fun at(route: String) {
        compose.waitUntil(timeoutMillis = 5_000) { nav.currentDestination?.route == route }
        compose.waitForIdle()
    }

    private fun waitFor(text: String, substring: Boolean = false) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty() }

    /** A side's line on the board, or another node TalkBack reads by its description. */
    private fun waitForSide(description: String) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty() }

    private fun gone(text: String) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isEmpty() }

    /** Taps what shows [text], or an icon labelled with it. */
    private fun tap(text: String) = compose.onNode(hasText(text) or hasContentDescription(text)).performClick()

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
        compose.onNode(hasText("Version", substring = true)).performScrollTo()
        repeat(DEVELOPER_TAPS) { compose.onNode(hasText("Version", substring = true)).performClick() }
        at(Routes.DEVELOPER)
    }

    @Test
    fun `every route opens, and Back leads back to the map`() {
        launch()

        visit("Today’s puzzle", Routes.DAILY)
        visit("Play a game", Routes.PLAY)
        visit("Rules", Routes.RULES)
        visit("Stats", Routes.STATS)
        visit("Shop", Routes.SHOP)
        visit("Settings", Routes.SETTINGS)

        // Level card, then Boosters over it: Back closes Boosters first.
        compose.onNodeWithContentDescription("Level 1-2, next").performClick()
        at(Routes.LEVEL)
        assertEquals(1, nav.currentBackStackEntry?.arguments?.getInt("world"))
        assertEquals(2, nav.currentBackStackEntry?.arguments?.getInt("number"))
        waitFor("World 1 · Level 2")
        // Boosters opens from a booster chip on the card of a level that allows one (LevelCardTest).
        compose.runOnUiThread { nav.navigate(Routes.BOOSTERS) }
        at(Routes.BOOSTERS)
        back()
        at(Routes.LEVEL)
        back()
        at(Routes.MAP)

        tap("Shop")
        at(Routes.SHOP)
        visit("A sample item", Routes.COSMETIC, returnTo = Routes.SHOP)
        back()
        at(Routes.MAP)

        tap("Settings")
        at(Routes.SETTINGS)
        compose.onNodeWithText("Credits and licences").performScrollTo()
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
        tap("Against the computer")
        at(Routes.GAME)
        waitForSide("You, score 0, to move")
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
        waitForSide("Red, score 0, to move")

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
    fun `Play the tutorial on How to play starts Level 1 as practice, which changes no progress, and Next plays Level 2`() {
        launch()
        visited.clear()
        tap("Rules")
        at(Routes.RULES)
        tap("Play the tutorial")
        at(Routes.GAME)
        assertEquals("1-1", savedLevel)
        assertTrue(app.games.saved(GameMode.CAMPAIGN)!!.practice)
        waitFor("Tap the circled point.")

        place(3, 4, size = 7)
        waitFor("You won!")
        tap("Next")
        compose.waitUntil(timeoutMillis = 5_000) { savedLevel == "1-2" }
        assertTrue(app.games.saved(GameMode.CAMPAIGN)!!.practice)
        assertEquals(Routes.GAME, nav.currentDestination?.route)
        assertEquals(LEVEL_1_PASSED, runBlocking { app.progress.progress.first() })

        // The map is under the tutorial: Back opens Pause, once Result has gone.
        gone("You won!")
        back()
        waitFor("To the map")
        tap("To the map")
        at(Routes.MAP)
        assertEquals(setOf(Routes.RULES, Routes.GAME, Routes.MAP), visited)
    }

    @Test
    fun `Replay the tutorial in Settings shows the skipped steps again`() {
        runBlocking { app.settings.update { it.copy(tutorialSkipped = true) } }
        launch()
        tap("Settings")
        at(Routes.SETTINGS)
        compose.onNodeWithText("Replay the tutorial").performScrollTo().performClick()
        at(Routes.GAME)
        waitFor("Tap the circled point.")
        assertEquals("1-1", savedLevel)
        assertTrue(app.games.saved(GameMode.CAMPAIGN)!!.practice)
        assertEquals(false, runBlocking { app.settings.settings.first() }.tutorialSkipped)

        tap("Skip")
        gone("Tap the circled point.")
        compose.waitUntil(timeoutMillis = 5_000) { runBlocking { app.settings.settings.first() }.tutorialSkipped }
    }

    @Test
    fun `every setting changed in Settings survives a restart`() {
        launch()
        tap("Settings")
        at(Routes.SETTINGS)
        waitForSide("Music")
        compose.onNodeWithContentDescription("Music").performSemanticsAction(SemanticsActions.SetProgress) { it(20f) }
        compose.onNodeWithContentDescription("Sounds").performSemanticsAction(SemanticsActions.SetProgress) { it(45f) }
        for (row in listOf("Vibration", "High-contrast pieces", "Threat highlights", "Drag")) {
            compose.onNodeWithText(row).performScrollTo().performClick()
        }
        val changed = AppSettings(musicVolume = 20, effectsVolume = 45, vibration = false, highContrast = true, threatHighlight = true, placing = PlacingMethod.DRAG)
        // Watched off the main thread: DataStore runs each save's change on the main thread, so
        // blocking it to read the store while saves are queued would wait forever.
        val stored = AtomicReference<AppSettings>()
        val watching = CoroutineScope(Dispatchers.IO).launch { app.settings.settings.collect { stored.set(it) } }
        compose.waitUntil(timeoutMillis = 5_000) { stored.get() == changed }
        watching.cancel()

        reopen(Routes.MAP)
        tap("Settings")
        at(Routes.SETTINGS)
        waitFor("Vibration")
        compose.onNodeWithContentDescription("Music").assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(20f, 0f..100f)))
        compose.onNodeWithContentDescription("Sounds").assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(45f, 0f..100f)))
        compose.onNodeWithText("Vibration").assertIsOff()
        compose.onNodeWithText("High-contrast pieces").performScrollTo().assertIsOn()
        compose.onNodeWithText("Threat highlights").performScrollTo().assertIsOn()
        compose.onNodeWithText("Drag").performScrollTo().assertIsSelected()
        assertEquals(changed, runBlocking { app.settings.settings.first() })
    }

    @Test
    fun `Settings opens each of its pages, and Back returns to Settings`() {
        launch()
        tap("Settings")
        at(Routes.SETTINGS)
        waitFor("Send feedback")
        for ((row, page) in listOf("Send feedback" to "feedback", "Privacy" to "privacy", "Play Games backup" to "backup", "Credits and licences" to "credits")) {
            compose.onNodeWithText(row).performScrollTo().performClick()
            at(Routes.SETTINGS_DETAILS)
            assertEquals(page, nav.currentBackStackEntry?.arguments?.getString("page"))
            back()
            at(Routes.SETTINGS)
        }
        compose.onNodeWithText("Credits and licences").performScrollTo().performClick()
        at(Routes.SETTINGS_DETAILS)
        tap("Open-source licences")
        compose.waitUntil(timeoutMillis = 5_000) { nav.currentBackStackEntry?.arguments?.getString("page") == "licences" }
        compose.waitForIdle()
        back()
        compose.waitUntil(timeoutMillis = 5_000) { nav.currentBackStackEntry?.arguments?.getString("page") == "credits" }
        compose.waitForIdle()
        tap("Back to settings")
        at(Routes.SETTINGS)
    }

    @Test
    fun `Back on the map leaves the app`() {
        launch()

        Espresso.pressBackUnconditionally()

        assertTrue(compose.activity.isFinishing)
    }

    /** Leaves the board for the map through Pause. */
    private fun toMap() {
        compose.onNodeWithContentDescription("Pause").performClick()
        tap("To the map")
        at(Routes.MAP)
    }

    @Test
    fun `free play and two players each keep their own saved game, and their cards resume them`() {
        launch()
        tap("Play a game")
        tap("Against the computer")
        at(Routes.GAME)
        waitForSide("You, score 0, to move")
        val free = app.games.saved(GameMode.FREE_PLAY)
        toMap()

        tap("Play a game")
        tap("Two players")
        at(Routes.GAME)
        waitForSide("Red, score 0, to move")
        val two = app.games.saved(GameMode.TWO_PLAYERS)
        toMap()
        // Starting two players kept the free game.
        assertSame(free, app.games.saved(GameMode.FREE_PLAY))

        tap("Play a game")
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasText("resume your game")).fetchSemanticsNodes().size == 2 }
        tap("Against the computer")
        at(Routes.GAME)
        waitForSide("You, score 0, to move")
        assertSame(free, app.games.saved(GameMode.FREE_PLAY))
        toMap()

        tap("Play a game")
        tap("Two players")
        waitForSide("Red, score 0, to move")
        assertSame(two, app.games.saved(GameMode.TWO_PLAYERS))
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `free play uses the strength reached in the campaign and the board size and length chosen on the note`() {
        launch(SampleProgress.midWorld)
        tap("Play a game")
        tap("Board 15 × 15 · Short · change")
        tap("12 × 12")
        tap("Open-ended")
        compose.waitUntil(timeoutMillis = 5_000) { runBlocking { app.settings.settings.first() }.length == GameLength.OPEN_ENDED }
        tap("Against the computer")
        at(Routes.GAME)
        waitForSide("You, score 0, to move")

        val game = app.games.saved(GameMode.FREE_PLAY)!!
        assertEquals(Campaign.strengthReached(SampleProgress.midWorld).text, game.opponent)
        val settings = (game.record as GameRecord).settings
        assertEquals(12, settings.width)
        assertEquals(null, settings.dotsPerPlayer)
        compose.onNodeWithText("Stop").assertExists()
    }

    @Test
    fun `the map opens the card of the level tapped, at the player's progress`() {
        launch()
        openDeveloperMenu()
        compose.onNodeWithText("Halfway through World 2").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { runBlocking { app.progress.progress.first() } == SampleProgress.midWorld }
        back()
        back()
        at(Routes.MAP)

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasContentDescription("Level 2-10, next")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Level 2-10, next").performClick()
        at(Routes.LEVEL)
        waitFor("World 2 · Level 10")
        back()
        at(Routes.MAP)

        compose.onNodeWithContentDescription("Level 2-9", substring = true).performClick()
        at(Routes.LEVEL)
        waitFor("World 2 · Level 9")
        back()
        at(Routes.MAP)

        // A locked level says what opens it, over the map.
        compose.onNodeWithContentDescription("Level 2-11, locked").performClick()
        waitFor("Pass level 2-10 to open this one.")
        tap("Close")
        gone("Pass level 2-10 to open this one.")
        assertEquals(Routes.MAP, nav.currentDestination?.route)
    }

    /** Taps board point ([x], [y]) twice, choose then place, on a board of [size] points across. */
    private fun place(x: Int, y: Int, size: Int) {
        repeat(2) {
            compose.onNodeWithTag(BOARD_TAG).performTouchInput {
                click(BoardGeometry(size, size, width.toFloat(), height.toFloat()).center(x, y))
            }
        }
    }

    @Test
    fun `a new player's first start opens Level 1, the first tap is a move, and Next shows the map for the first time`() {
        launch(Progress.NONE, route = Routes.GAME)
        assertEquals("1-1", savedLevel)
        waitForSide("You, score 0, to move")

        // Level 1-1: the ring around the computer's dot needs one more dot, below it.
        place(3, 4, size = 7)
        waitFor("You won!")
        compose.waitUntil(timeoutMillis = 5_000) { runBlocking { app.progress.progress.first() }.stars["1-1"] == 3 }
        waitFor("Level 1-2 is open")
        compose.onNodeWithContentDescription("3 of 3 stars").assertExists()
        // No map and no Level card on the way.
        assertEquals(setOf(Routes.LOADING, Routes.GAME), visited)

        tap("Next")
        at(Routes.LEVEL)
        assertEquals(2, nav.currentBackStackEntry?.arguments?.getInt("number"))
        waitFor("World 1 · Level 2")
        back()
        at(Routes.MAP)
        Espresso.pressBackUnconditionally()
        assertTrue(compose.activity.isFinishing)
    }

    @Test
    fun `a return before Level 1 is passed opens it where the player left off`() {
        // Level 1 left after a dot away from the ring and the computer's reply.
        val first = Campaign.levels.first()
        val start = levelGame(first, threatHighlight = false, now = 0)
        val saved = start.copy(record = start.record.withActions(listOf(Action.Place(Pos(0, 0)), Action.Place(Pos(6, 6)))))
        app.games.start(saved)

        launch(Progress.NONE, route = Routes.GAME)

        assertEquals(setOf(Routes.LOADING, Routes.GAME), visited)
        val resumed = app.games.saved(GameMode.CAMPAIGN)!!
        assertEquals(saved.startedAt, resumed.startedAt)
        assertEquals(saved.record.actions, resumed.record.actions)
        waitForSide("You, score 0, to move")

        // To the map shows the map early; the next start still opens Level 1.
        compose.onNodeWithContentDescription("Pause").performClick()
        tap("To the map")
        at(Routes.MAP)
        compose.onNodeWithContentDescription("Level 1-1, next").assertExists()
        reopen(Routes.GAME)
        assertEquals(saved.startedAt, app.games.saved(GameMode.CAMPAIGN)?.startedAt)
    }

    @Test
    fun `a return after Level 1 is passed opens the map`() {
        launch(LEVEL_1_PASSED, route = Routes.MAP)
        assertEquals(setOf(Routes.LOADING, Routes.MAP), visited)
        compose.onNodeWithContentDescription("Level 1-2, next").assertExists()

        // A level game left for the map stays saved, and the app still opens on the map.
        compose.onNodeWithContentDescription("Level 1-2, next").performClick()
        at(Routes.LEVEL)
        tap("Start")
        at(Routes.GAME)
        compose.onNodeWithContentDescription("Pause").performClick()
        tap("To the map")
        at(Routes.MAP)
        reopen(Routes.MAP)
        assertEquals(setOf(Routes.LOADING, Routes.MAP), visited)
        assertEquals("1-2", savedLevel)
    }

    @Test
    fun `a level left for the map is resumed from its card`() {
        launch()
        compose.onNode(hasContentDescription("Level 1-1,", substring = true)).performClick()
        at(Routes.LEVEL)
        tap("Start")
        at(Routes.GAME)
        waitForSide("You, score 0, to move")
        val saved = app.games.saved(GameMode.CAMPAIGN)
        compose.onNodeWithContentDescription("Pause").performClick()
        tap("To the map")
        at(Routes.MAP)

        compose.onNode(hasContentDescription("Level 1-1,", substring = true)).performClick()
        at(Routes.LEVEL)
        waitFor("Resume")
        tap("Resume")
        at(Routes.GAME)
        assertSame(saved, app.games.saved(GameMode.CAMPAIGN))
    }

    /**
     * Off the main thread, while the main thread keeps running: a save a screen started may still
     * be queued, and DataStore runs each save's change on the thread that asked for it, here the
     * main thread. Blocking it until the reset is done would wait forever.
     */
    @After
    fun `forget the progress and the settings`() {
        val reset = CoroutineScope(Dispatchers.IO).launch {
            app.progress.replace(Progress.NONE)
            app.settings.update { AppSettings() }
        }
        compose.waitUntil(timeoutMillis = 10_000) { reset.isCompleted }
    }

    private companion object {
        /** Level 1 passed with one star: the app opens on the map, and a replay can earn more. */
        val LEVEL_1_PASSED = Progress(mapOf("1-1" to 1))
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
