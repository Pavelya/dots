package io.github.pavelya.dots.ui.screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import dots.campaign.Campaign
import dots.engine.GameSettings
import dots.engine.Opening
import dots.engine.Player
import dots.engine.Pos
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.gameViewModel
import io.github.pavelya.dots.game.levelGame
import io.github.pavelya.dots.ui.board.BoardGeometry
import io.github.pavelya.dots.ui.theme.DotsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Both ways of placing a dot (decision 0024), by finger on the game screen: two people on a
 * 5 x 5 board, so every touch is Red's or Blue's own. On a 360 x 800 dp phone the board's points are 72 dp apart.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class PlacingMethodsTest {
    @get:Rule
    val compose = createComposeRule()

    private val settings = GameSettings(width = 5, height = 5, dotsPerPlayer = 3, opening = Opening.EMPTY)

    private fun open(method: PlacingMethod): GameViewModel {
        val vm = gameViewModel(newSettings = { settings })
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = {}, onRules = {}, placing = method) } }
        return vm
    }

    private fun touch(columns: Int = settings.width, rows: Int = settings.height, block: TouchInjectionScope.(BoardGeometry) -> Unit) {
        compose.onNodeWithTag(BOARD_TAG).performTouchInput {
            block(BoardGeometry(columns, rows, width.toFloat(), height.toFloat()))
        }
        compose.waitForIdle()
    }

    /**
     * Reef (4-18), a level with holes and blocked points, between two people: its row 2 has a
     * blocked point at (6, 2) and a hole at (11, 2), each with a free point beside it.
     */
    private val reef = Campaign.level(4, 18)

    private fun openReef(method: PlacingMethod): GameViewModel {
        val entry = levelGame(reef, threatHighlight = false, now = 0)
        val vm = GameViewModel(start = entry, nextGame = { entry }, computerFor = { null }, clock = { 0L })
        val state = vm.ui.value.state
        assertTrue(state.isBlocked(ROCK) && state.isHole(HOLE) && state.isLegal(BESIDE_ROCK) && state.isLegal(BESIDE_HOLE))
        compose.setContent { DotsTheme { GameScreen(vm, onToMap = {}, onRules = {}, placing = method) } }
        return vm
    }

    private fun touchReef(block: TouchInjectionScope.(BoardGeometry) -> Unit) = touch(reef.map.width, reef.map.height, block)

    private fun GameViewModel.owner(x: Int, y: Int): Player? = ui.value.state.ownerAt(Pos(x, y))

    @Test
    fun `two taps, the first shows the preview, the second places`() {
        val vm = open(PlacingMethod.TWO_TAPS)
        touch { click(it.center(1, 1)) }
        assertEquals(Pos(1, 1), vm.ui.value.preview)
        assertNull(vm.owner(1, 1))

        touch { click(it.center(1, 1)) }
        assertEquals(Player.RED, vm.owner(1, 1))
    }

    @Test
    fun `drag, the preview follows the finger, and lifting it places the dot`() {
        val vm = open(PlacingMethod.DRAG)
        touch { down(it.center(1, 1)) }
        assertEquals(Pos(1, 1), vm.ui.value.preview)

        touch { moveTo(it.center(2, 1)) }
        assertEquals(Pos(2, 1), vm.ui.value.preview)
        assertNull(vm.owner(2, 1))

        touch { up() }
        assertEquals(Player.RED, vm.owner(2, 1))
        assertNull(vm.owner(1, 1))
    }

    @Test
    fun `drag, lifting the finger off the board places nothing`() {
        val vm = open(PlacingMethod.DRAG)
        touch { board ->
            down(board.center(3, 3))
            moveTo(Offset(board.center(3, 4).x, height + 60f)) // just below the board
            up()
        }
        assertEquals(0, vm.ui.value.state.moveCount)
        assertNull(vm.ui.value.preview)
    }

    @Test
    fun `drag, the dot goes where the finger is lifted, not where it went down`() {
        val vm = open(PlacingMethod.DRAG)
        touch { board ->
            down(board.center(0, 0))
            moveTo(board.center(1, 0))
            moveTo(board.center(1, 1))
        }
        assertEquals(Pos(1, 1), vm.ui.value.preview)
        touch { up() }
        assertEquals(Player.RED, vm.owner(1, 1))
        assertEquals(1, vm.ui.value.state.moveCount)
    }

    @Test
    fun `two taps on a hole or a blocked point, no preview and no dot`() {
        val vm = openReef(PlacingMethod.TWO_TAPS)
        for (point in listOf(HOLE, ROCK)) {
            touchReef { click(it.center(point.x, point.y)) }
            assertNull(vm.ui.value.preview)
            touchReef { click(it.center(point.x, point.y)) }
            assertNull(vm.ui.value.preview)
            assertEquals(0, vm.ui.value.state.moveCount)
        }
        // A preview on a free point goes when the next tap is on a hole, and no dot is placed.
        touchReef { click(it.center(BESIDE_HOLE.x, BESIDE_HOLE.y)) }
        assertEquals(BESIDE_HOLE, vm.ui.value.preview)
        touchReef { click(it.center(HOLE.x, HOLE.y)) }
        assertNull(vm.ui.value.preview)
        assertEquals(0, vm.ui.value.state.moveCount)
    }

    @Test
    fun `drag over a hole or a blocked point, no preview, and lifting there places nothing`() {
        val vm = openReef(PlacingMethod.DRAG)
        for ((beside, point) in listOf(BESIDE_HOLE to HOLE, BESIDE_ROCK to ROCK)) {
            touchReef { down(it.center(beside.x, beside.y)) }
            assertEquals(beside, vm.ui.value.preview)
            touchReef { moveTo(it.center(point.x, point.y)) }
            assertNull(vm.ui.value.preview)
            touchReef { up() }
            assertNull(vm.ui.value.preview)
            assertEquals(0, vm.ui.value.state.moveCount)
            assertNull(vm.ui.value.state.ownerAt(beside))
        }
    }

    private companion object {
        val ROCK = Pos(6, 2)
        val BESIDE_ROCK = Pos(5, 2)
        val HOLE = Pos(11, 2)
        val BESIDE_HOLE = Pos(10, 2)
    }
}
