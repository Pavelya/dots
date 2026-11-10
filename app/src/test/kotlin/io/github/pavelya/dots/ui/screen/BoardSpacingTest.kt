package io.github.pavelya.dots.ui.screen

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** The board's spacing from docs/design/flows.md, Wireframes, the layout rules. */
class BoardSpacingTest {
    /** The height left for the board: the screen less 24 dp of status bar, 12 dp of gesture bar, the top row and the player's line and tool bar. */
    private fun left(screen: Int) = (screen - 24 - 12 - 56 - 120).dp

    @Test
    fun `a 15 x 15 board is 24 dp on 360 x 800 and 21 dp on 320 x 568`() {
        assertEquals(24.dp, boardSpacing(360.dp, left(800), 15, 15))
        assertEquals(21.dp, boardSpacing(320.dp, left(568), 15, 15))
    }

    @Test
    fun `a 20 x 20 board takes the next whole dp that fits`() {
        assertEquals(18.dp, boardSpacing(360.dp, left(800), 20, 20))
    }

    @Test
    fun `with three-button navigation on a short phone the board takes a smaller spacing`() {
        // 48 dp of navigation bar instead of 12 leaves 320 dp of height on a 360 x 568 phone.
        assertEquals(21.dp, boardSpacing(360.dp, (568 - 24 - 48 - 56 - 120).dp, 15, 15))
    }
}
