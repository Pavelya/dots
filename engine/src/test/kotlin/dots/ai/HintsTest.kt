package dots.ai

import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HintsTest {
    private val position = GameState.fromAscii(
        """
        .......
        ..R....
        .RBR...
        .......
        ....B..
        ...B.R.
        .......
        """,
    )

    @Test
    fun `a group one dot from capture names the closing point`() {
        val dangers = Hints.dangers(position, Player.BLUE)

        val near = dangers.first()
        assertEquals(listOf(Pos(2, 2)), near.group)
        assertEquals(1, near.dotsNeeded)
        assertEquals(listOf(Pos(2, 3)), near.closingPoints)
    }

    @Test
    fun `groups further away than the limit are not reported`() {
        assertTrue(Hints.dangers(position, Player.BLUE, maxDots = 1).all { it.dotsNeeded == 1 })
        assertTrue(Hints.dangers(position, Player.RED, maxDots = 1).isEmpty())
    }

    @Test
    fun `threats the opponent has no dots left for are left out`() {
        val noDotsForRed = GameState.fromAscii(
            """
            .......
            ..R....
            .RBR...
            .......
            .......
            """,
            dotsPerPlayer = 1,
        ).play(Pos(6, 4)).state // red uses its only dot elsewhere

        assertTrue(Hints.dangers(noDotsForRed, Player.BLUE).isEmpty())
    }
}
