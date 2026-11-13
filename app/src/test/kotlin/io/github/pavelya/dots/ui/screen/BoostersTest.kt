package io.github.pavelya.dots.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso
import dots.ai.Ai
import dots.campaign.Level
import dots.campaign.LevelMap
import dots.campaign.Mission
import dots.campaign.Strength
import dots.engine.Action
import dots.engine.Booster
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.levelGame
import io.github.pavelya.dots.ui.board.BoardGeometry
import io.github.pavelya.dots.ui.theme.DotsTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The five boosters on the game screen (A-05): each from its tool, through its preview, with
 * Confirm, Cancel and Back; a booster with none left; and the point chosen by either placing
 * method (decision 0024).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BoostersTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** A 7 x 7 board; the computer has dots at (3, 3) and (5, 5). */
    private val map = LevelMap(listOf(".......", ".......", ".......", "...B...", ".......", ".....B.", "......."))

    /** Plays the first free point, in the top row, far from where the tests aim. */
    private val topRow = object : Ai {
        override fun choose(state: GameState): Action = Action.Place(state.legalMoves().first())
    }

    private val hintPoint = Pos(6, 6)

    private fun game(boosters: Map<Booster, Int> = Booster.entries.associateWith { 1 }): GameViewModel {
        val level = Level(1, 7, "Every Booster", Strength.BEGINNER, 6, map, Mission.Win, boosters = boosters)
        return GameViewModel(
            start = levelGame(level, threatHighlight = false, now = 0),
            nextGame = { levelGame(level, threatHighlight = false, now = 0) },
            computerFor = { topRow },
            computerDispatcher = Dispatchers.Unconfined,
            replyDelayMillis = 0,
            clock = { 0L },
            hintFor = { _, _ -> Action.Hint(hintPoint) },
        )
    }

    private fun show(vm: GameViewModel, placing: PlacingMethod = PlacingMethod.TWO_TAPS) {
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = {}, onRules = {}, placing = placing) } }
        yourTurn(vm)
    }

    private fun yourTurn(vm: GameViewModel) =
        compose.waitUntil(timeoutMillis = 5_000) { vm.ui.value.state.toMove == Player.RED && !vm.ui.value.thinking }

    private fun center(x: Int, y: Int, width: Int, height: Int) =
        BoardGeometry(map.width, map.height, width.toFloat(), height.toFloat()).center(x, y)

    private fun tapBoard(x: Int, y: Int) =
        compose.onNodeWithTag(BOARD_TAG).performTouchInput { click(center(x, y, width, height)) }

    private fun place(x: Int, y: Int) = repeat(2) { tapBoard(x, y) }

    private fun tool(description: String) = compose.onNodeWithContentDescription(description)

    private fun GameViewModel.left(booster: Booster) = ui.value.state.boostersLeft(Player.RED, booster)

    @Test
    fun `a Wall is aimed by a tap and put up by its button`() {
        val vm = game()
        show(vm)
        tool("Wall, 1 left").assertIsEnabled().performClick()

        compose.onNodeWithText("Wall blocks one empty point.").assertIsDisplayed()
        compose.onNodeWithText("Tap a point").assertIsNotEnabled()
        tapBoard(2, 2)
        assertEquals(Pos(2, 2), vm.ui.value.aim?.point)
        compose.onNodeWithText("Block it").assertIsEnabled().performClick()

        assertTrue(vm.ui.value.state.isBlocked(Pos(2, 2)))
        compose.onNodeWithText("Block it").assertDoesNotExist()
        tool("Wall, none left").assertIsNotEnabled()
        // One booster a turn: the others wait for the next turn.
        tool("Bomb, 1 left").assertIsNotEnabled()
    }

    @Test
    fun `under drag a Bomb is aimed by lifting the finger, and lifting it off the board keeps the point`() {
        val vm = game()
        show(vm, PlacingMethod.DRAG)
        tool("Bomb, 1 left").performClick()
        compose.onNodeWithText("Drag to a point").assertIsNotEnabled()

        compose.onNodeWithTag(BOARD_TAG).performTouchInput {
            down(center(1, 1, width, height))
            moveTo(center(3, 3, width, height))
            up()
        }
        assertEquals(Pos(3, 3), vm.ui.value.aim?.point)
        compose.onNodeWithTag(BOARD_TAG).performTouchInput {
            down(center(5, 4, width, height))
            moveTo(Offset(center(5, 4, width, height).x, height + 60f)) // just below the board
            up()
        }
        assertEquals(Pos(3, 3), vm.ui.value.aim?.point)

        compose.onNodeWithText("Bomb it").performClick()
        assertNull(vm.ui.value.state.ownerAt(Pos(3, 3)))
        assertEquals(Player.BLUE, vm.ui.value.state.ownerAt(Pos(5, 5)))
        assertEquals(0, vm.left(Booster.BOMB))
    }

    @Test
    fun `under two taps a Bomb is aimed by one tap, and a tap off the board keeps the point`() {
        val vm = game()
        show(vm)
        tool("Bomb, 1 left").performClick()
        tapBoard(5, 4)
        tapBoard(3, 3) // the computer's dot is a target too: it moves the point
        tapBoard(0, 0) // too far from any dot: keeps it
        assertEquals(Pos(3, 3), vm.ui.value.aim?.point)
        tapBoard(5, 4)
        assertEquals(Pos(5, 4), vm.ui.value.aim?.point)
        compose.onNodeWithText("Bomb it").performClick()
        assertNull(vm.ui.value.state.ownerAt(Pos(5, 5)))
    }

    @Test
    fun `Cancel and Back close the preview and spend nothing`() {
        val vm = game()
        show(vm)
        tool("Hint, 1 left").performClick()
        compose.onNodeWithText("Hint shows your best move.").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertNull(vm.ui.value.aim)
        tool("Hint, 1 left").assertIsEnabled()

        tool("Double dot, 1 left").performClick()
        compose.onNodeWithText("Use it").assertIsDisplayed()
        Espresso.pressBack()
        compose.waitForIdle()
        assertNull(vm.ui.value.aim)
        compose.onNodeWithText("Paused").assertDoesNotExist()
        assertEquals(1, vm.left(Booster.DOUBLE_DOT))
    }

    @Test
    fun `a Hint circles the best move`() {
        val vm = game()
        show(vm)
        tool("Hint, 1 left").performClick()
        compose.onNodeWithText("Show me").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { vm.ui.value.hint == hintPoint }
        tool("Hint, none left").assertIsNotEnabled()
        place(hintPoint.x, hintPoint.y)
        compose.waitUntil(timeoutMillis = 5_000) { vm.ui.value.hint == null }
    }

    @Test
    fun `a Double dot shows two dots to place, then one`() {
        val vm = game()
        show(vm)
        tool("Double dot, 1 left").performClick()
        compose.onNodeWithText("Use it").performClick()
        compose.onNodeWithContentDescription("2 dots this turn").assertIsDisplayed()
        place(2, 2)
        compose.onNodeWithContentDescription("1 more dot this turn").assertIsDisplayed()
        place(2, 4)
        yourTurn(vm)
        compose.onNodeWithContentDescription("1 more dot this turn").assertDoesNotExist()
        assertEquals(Player.RED, vm.ui.value.state.ownerAt(Pos(2, 4)))
    }

    @Test
    fun `Undo waits for the first move, then takes back the turn`() {
        val vm = game()
        show(vm)
        tool("Undo, 1 left, after your first move").assertIsNotEnabled()
        place(4, 4)
        yourTurn(vm)

        tool("Undo, 1 left").assertIsEnabled().performClick()
        compose.onNodeWithText("Undo takes back the pale dots.").assertIsDisplayed()
        compose.onNodeWithText("Take it back?").assertIsDisplayed()
        compose.onNodeWithText("Take back").performClick()

        assertNull(vm.ui.value.state.ownerAt(Pos(4, 4)))
        assertEquals(0, vm.ui.value.state.dotsPlaced(Player.BLUE))
        tool("Undo, none left").assertIsNotEnabled()
    }

    @Test
    fun `boosters the level does not give show dashed with none left, and do nothing`() {
        val vm = game(boosters = mapOf(Booster.UNDO to 1))
        show(vm)
        for (name in listOf("Wall", "Hint", "Bomb", "Double dot")) tool("$name, none left").assertIsNotEnabled()
        tool("Bomb, none left").performClick()
        assertNull(vm.ui.value.aim)
        compose.onAllNodes(hasText("Cancel")).fetchSemanticsNodes().let { assertTrue(it.isEmpty()) }
        compose.onNode(hasContentDescription("Undo, 1 left", substring = true)).assertExists()
    }
}
