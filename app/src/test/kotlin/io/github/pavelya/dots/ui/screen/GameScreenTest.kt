package io.github.pavelya.dots.ui.screen

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso
import dots.ai.Ai
import dots.engine.Action
import dots.engine.GameSettings
import dots.engine.GameState
import dots.engine.Opening
import io.github.pavelya.dots.game.gameViewModel
import io.github.pavelya.dots.ui.board.BoardGeometry
import io.github.pavelya.dots.ui.theme.DotsTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch

/** On a 360 x 800 dp phone, where the computer's face fits above the board. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class GameScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val settings = GameSettings(width = 5, height = 5, dotsPerPlayer = 2, opening = Opening.EMPTY)

    private val firstFreePoint = object : Ai {
        override fun choose(state: GameState): Action = Action.Place(state.legalMoves().first())
    }

    /** Taps a board point twice (choose, then place), as a player would. */
    private fun place(x: Int, y: Int) {
        repeat(2) {
            compose.onNodeWithTag(BOARD_TAG).performTouchInput {
                click(BoardGeometry(settings.width, settings.height, width.toFloat(), height.toFloat()).center(x, y))
            }
        }
    }

    private fun waitFor(matcher: SemanticsMatcher) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private fun waitFor(text: String) = waitFor(hasText(text))

    private fun gone(matcher: SemanticsMatcher) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isEmpty() }

    /** A side's line under or above the board, as TalkBack reads it. */
    private fun side(text: String) = hasContentDescription(text)

    @Test
    fun `a whole game against the computer ends on the result screen, and play again restarts`() {
        val vm = computerGame()
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = {}, onRules = {}) } }
        waitFor(side("You, score 0, to move"))
        compose.onNodeWithContentDescription("2 dots left").assertIsDisplayed()

        playToTheEnd()
        compose.onNodeWithText("Play again").assertIsDisplayed().performClick()
        waitFor(side("You, score 0, to move"))
        compose.onNodeWithContentDescription("2 dots left").assertIsDisplayed()
        compose.onNodeWithText("Undo").assertIsDisplayed()
    }

    private fun playToTheEnd() {
        place(4, 4)
        waitFor(hasContentDescription("1 dot left"))
        waitFor(side("You, score 0, to move"))
        place(4, 3)
        waitFor("A draw, 0 to 0")
    }

    private fun computerGame(computer: Ai = firstFreePoint) = gameViewModel(
        newSettings = { settings },
        newComputer = { computer },
        computerDispatcher = Dispatchers.Default,
        replyDelayMillis = 0,
    )

    @Test
    fun `the top row names the game, and the computer sits above the board with its face`() {
        compose.setContent { DotsTheme { GameScreen(computerGame(), onToMap = {}, onRules = {}) } }

        compose.onNodeWithText("Free play").assertIsDisplayed()
        compose.onNode(side("Computer, score 0")).assertIsDisplayed()
        compose.onNodeWithText("Good luck!").assertIsDisplayed()
    }

    @Test
    fun `Map and Back on the result both go to the map`() {
        var toMap = 0
        compose.setContent { DotsTheme { GameScreen(computerGame(), onToMap = { toMap++ }, onRules = {}) } }
        playToTheEnd()

        compose.onNodeWithText("Map").performClick()
        Espresso.pressBack()
        compose.waitForIdle()

        assertEquals(2, toMap)
    }

    @Test
    fun `Pause holds Resume, Restart, To the map, sound and How to play`() {
        var sound = true
        var toMap = 0
        var rules = 0
        compose.setContent {
            DotsTheme {
                GameScreen(computerGame(), onToMap = { toMap++ }, onRules = { rules++ }, sound = sound, onSound = { sound = it })
            }
        }

        compose.onNodeWithContentDescription("Pause").performClick()
        compose.onNodeWithText("Paused").assertIsDisplayed()
        compose.onNodeWithText("Resume").assertIsDisplayed()
        compose.onNodeWithText("Restart").assertIsDisplayed()
        compose.onNodeWithContentDescription("How to play").performClick()
        compose.onNodeWithContentDescription("Sound on").performClick()
        compose.onNodeWithText("To the map").performClick()
        assertEquals(false, sound)
        assertEquals(1, rules)
        assertEquals(1, toMap)

        compose.onNodeWithText("Resume").performClick()
        gone(hasText("Paused"))
        compose.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun `Back opens Pause and Back again resumes`() {
        compose.setContent { DotsTheme { GameScreen(computerGame(), onToMap = {}, onRules = {}) } }

        Espresso.pressBack()
        waitFor("Paused")
        Espresso.pressBack()
        gone(hasText("Paused"))
    }

    @Test
    fun `Restart asks first, then starts a new game`() {
        val vm = computerGame()
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = {}, onRules = {}) } }
        place(4, 4)
        waitFor(hasContentDescription("1 dot left"))

        compose.onNodeWithContentDescription("Pause").performClick()
        compose.onNodeWithText("Restart").performClick()
        compose.onNodeWithText("Keep playing").performClick()
        waitFor("Resume") // back in Pause, the game unchanged
        compose.onNodeWithText("Resume").performClick()
        compose.onNodeWithContentDescription("1 dot left").assertExists()

        compose.onNodeWithContentDescription("Pause").performClick()
        compose.onNodeWithText("Restart").performClick()
        compose.onNodeWithText("Start a new game? This one will be lost.").assertIsDisplayed()
        Espresso.pressBack() // Back while it asks returns to Pause
        waitFor("Resume")
        compose.onNodeWithText("Restart").performClick()
        compose.onNodeWithText("Restart").performClick()
        waitFor(hasContentDescription("2 dots left"))
    }

    @Test
    fun `on the computer's turn the pen stroke moves to it, taps are ignored and Undo waits`() {
        val go = CountDownLatch(1)
        val slow = object : Ai {
            override fun choose(state: GameState): Action {
                go.await()
                return firstFreePoint.choose(state)
            }
        }
        val vm = computerGame(slow)
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = {}, onRules = {}) } }
        place(4, 4)

        waitFor(side("Computer, score 0, to move"))
        compose.onNode(side("You, score 0")).assertIsDisplayed()
        compose.onNodeWithText("Undo").assertIsNotEnabled()
        place(4, 3) // ignored: not the player's turn
        compose.onNodeWithContentDescription("1 dot left").assertExists()

        go.countDown()
        waitFor(side("You, score 0, to move"))
        compose.onNode(side("Computer, score 0")).assertIsDisplayed()
    }

    @Test
    fun `two players see both names in their inks, the turn mark, and a banner at each change`() {
        val vm = gameViewModel(newSettings = { settings })
        compose.mainClock.autoAdvance = false
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = {}, onRules = {}) } }
        compose.mainClock.advanceTimeByFrame()

        compose.onNodeWithText("Two players").assertIsDisplayed()
        compose.onNode(side("Red, score 0, to move")).assertIsDisplayed()
        compose.onNode(side("Blue, score 0")).assertIsDisplayed()
        compose.onNodeWithText("Red’s turn").assertDoesNotExist() // no banner for the first turn
        compose.onNodeWithText("Undo").assertDoesNotExist() // no undo between two people (decision 0023)

        place(4, 4)
        compose.mainClock.advanceTimeBy(100) // the banner shows a frame after the turn changes
        compose.onNodeWithText("Blue’s turn").assertIsDisplayed()
        compose.onNode(side("Blue, score 0, to move")).assertIsDisplayed()
        compose.onNode(side("Red, score 0")).assertIsDisplayed()

        compose.mainClock.advanceTimeBy(BANNER_MILLIS)
        compose.onNodeWithText("Blue’s turn").assertDoesNotExist()

        place(0, 0)
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Red’s turn").assertIsDisplayed()
    }
}
