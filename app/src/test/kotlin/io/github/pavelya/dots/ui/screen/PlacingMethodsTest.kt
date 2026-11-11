package io.github.pavelya.dots.ui.screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import dots.engine.GameSettings
import dots.engine.Opening
import dots.engine.Player
import dots.engine.Pos
import io.github.pavelya.dots.data.PlacingMethod
import io.github.pavelya.dots.game.GameViewModel
import io.github.pavelya.dots.game.gameViewModel
import io.github.pavelya.dots.ui.board.BoardGeometry
import io.github.pavelya.dots.ui.board.HOLD_MILLIS
import io.github.pavelya.dots.ui.board.ZOOM_SPACING
import io.github.pavelya.dots.ui.board.zoomFactor
import io.github.pavelya.dots.ui.theme.DotsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Each way of placing a dot (A-13), by finger on the game screen: two people on a 5 x 5 board,
 * so every touch is Red's or Blue's own. On a 360 x 800 dp phone the board's points are 72 dp apart.
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

    private fun touch(block: TouchInjectionScope.(BoardGeometry) -> Unit) {
        compose.onNodeWithTag(BOARD_TAG).performTouchInput {
            block(BoardGeometry(settings.width, settings.height, width.toFloat(), height.toFloat()))
        }
        compose.waitForIdle()
    }

    private fun GameViewModel.owner(x: Int, y: Int): Player? = ui.value.state.ownerAt(Pos(x, y))

    @Test
    fun `two taps: the first shows the preview, the second places`() {
        val vm = open(PlacingMethod.TWO_TAPS)
        touch { click(it.center(1, 1)) }
        assertEquals(Pos(1, 1), vm.ui.value.preview)
        assertNull(vm.owner(1, 1))

        touch { click(it.center(1, 1)) }
        assertEquals(Player.RED, vm.owner(1, 1))
    }

    @Test
    fun `zoom: the first tap zooms in around the point, the next tap places where it lands`() {
        val vm = open(PlacingMethod.ZOOM)
        touch { click(it.center(1, 2)) }
        assertEquals(Pos(1, 2), vm.ui.value.preview)

        // Zoomed in, the point to the right of the preview is further away than before.
        touch { board ->
            val zoom = zoomFactor(board.spacing, ZOOM_SPACING.toPx())
            click(board.zoomed(zoom, Pos(1, 2)).center(2, 2))
        }
        assertEquals(Player.RED, vm.owner(2, 2))
        assertNull(vm.owner(1, 2))
        assertNull(vm.ui.value.preview)

        // Zoomed out again: a tap lands where the board shows unzoomed.
        touch { click(it.center(4, 4)) }
        assertEquals(Pos(4, 4), vm.ui.value.preview)
        touch { click(it.center(4, 4)) } // the zoom keeps (4, 4) where it was
        assertEquals(Player.BLUE, vm.owner(4, 4))
    }

    @Test
    fun `zoom: a tap on a taken point zooms out and places nothing`() {
        val vm = open(PlacingMethod.ZOOM)
        touch { click(it.center(2, 2)) }
        touch { click(it.center(2, 2)) }
        assertEquals(Player.RED, vm.owner(2, 2))

        touch { click(it.center(2, 3)) }
        touch { board -> click(board.zoomed(zoomFactor(board.spacing, ZOOM_SPACING.toPx()), Pos(2, 3)).center(2, 2)) }
        assertNull(vm.ui.value.preview)
        assertEquals(1, vm.ui.value.state.moveCount)
    }

    @Test
    fun `magnifier: the preview follows the finger, and lifting it places the dot`() {
        val vm = open(PlacingMethod.MAGNIFIER)
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
    fun `magnifier: lifting the finger off the board places nothing`() {
        val vm = open(PlacingMethod.MAGNIFIER)
        touch { board ->
            down(board.center(3, 3))
            moveTo(Offset(board.center(3, 4).x, height + 60f)) // just below the board
            up()
        }
        assertEquals(0, vm.ui.value.state.moveCount)
        assertNull(vm.ui.value.preview)
    }

    @Test
    fun `drag: press, drag and lift places the dot where the finger leaves the board`() {
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
    fun `long press: holding the point places the dot, a short touch does not`() {
        val vm = open(PlacingMethod.LONG_PRESS)
        touch { board ->
            down(board.center(3, 1))
            advanceEventTime(HOLD_MILLIS / 2)
            up()
        }
        assertNull(vm.owner(3, 1))
        assertNull(vm.ui.value.preview)

        touch { board ->
            down(board.center(3, 1))
            advanceEventTime(HOLD_MILLIS + 100)
            up()
        }
        assertEquals(Player.RED, vm.owner(3, 1))
        assertEquals(1, vm.ui.value.state.moveCount)
    }

    @Test
    fun `long press: moving to another point starts the hold again there`() {
        val vm = open(PlacingMethod.LONG_PRESS)
        touch { board ->
            down(board.center(0, 3))
            advanceEventTime(HOLD_MILLIS - 100)
            moveTo(board.center(1, 3))
            advanceEventTime(HOLD_MILLIS - 100)
            up()
        }
        assertEquals(0, vm.ui.value.state.moveCount)

        touch { board ->
            down(board.center(0, 3))
            advanceEventTime(HOLD_MILLIS - 100)
            moveTo(board.center(1, 3))
            advanceEventTime(HOLD_MILLIS + 100)
            up()
        }
        assertEquals(Player.RED, vm.owner(1, 3))
        assertNull(vm.owner(0, 3))
    }
}
