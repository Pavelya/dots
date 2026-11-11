package io.github.pavelya.dots.ui.screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.click
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
import io.github.pavelya.dots.ui.theme.DotsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private fun touch(block: TouchInjectionScope.(BoardGeometry) -> Unit) {
        compose.onNodeWithTag(BOARD_TAG).performTouchInput {
            block(BoardGeometry(settings.width, settings.height, width.toFloat(), height.toFloat()))
        }
        compose.waitForIdle()
    }

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
}
