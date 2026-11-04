package io.github.pavelya.dots.game

import dots.engine.GameSettings
import dots.engine.Player
import dots.engine.Pos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameViewModelTest {
    private fun viewModel() = GameViewModel(GameSettings.POC.copy(seed = 1))

    @Test
    fun `a dot is placed only on the second tap, then it is the other player's turn`() {
        val vm = viewModel()
        val corner = Pos(0, 0)

        vm.onTap(corner)
        assertEquals(corner, vm.ui.value.preview)
        assertNull(vm.ui.value.state.ownerAt(corner))

        vm.onTap(corner)
        val ui = vm.ui.value
        assertEquals(Player.RED, ui.state.ownerAt(corner))
        assertEquals(corner, ui.lastMove)
        assertNull(ui.preview)
        assertEquals(Player.BLUE, ui.state.toMove)
        assertEquals(32, ui.state.dotsLeft(Player.RED))
    }

    @Test
    fun `tapping an occupied point clears the preview and changes nothing`() {
        val vm = viewModel()
        vm.onTap(Pos(0, 0))
        vm.onTap(Pos(0, 0))
        vm.onTap(Pos(14, 14))

        vm.onTap(Pos(0, 0))

        assertNull(vm.ui.value.preview)
        assertEquals(Player.BLUE, vm.ui.value.state.toMove)
    }
}
