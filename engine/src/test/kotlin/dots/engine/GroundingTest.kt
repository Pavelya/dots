package dots.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroundingTest {
    private fun p(name: String) = Notation.parse(name)!!

    /** Red's a2 and b3 form one group that touches the border, but only across a diagonal step. */
    private val diagonalStep = GameState.fromAscii(
        """
        .....
        R.B..
        BRB..
        .B...
        .....
        """,
        first = Player.BLUE,
    )

    @Test
    fun `a group touching the border loses the dot it reaches only diagonally`() {
        val state = diagonalStep

        assertEquals(listOf(setOf(p("a2"), p("b3"))), Grounding.groups(state, Player.RED).map { it.toSet() })
        assertEquals(setOf(p("a2")), Grounding.safe(state, Player.RED))
        assertEquals(listOf(listOf(p("b3"))), Grounding.exposed(state, Player.RED))

        // Blue closes its own diagonal step (a3 to b2) through the same spot and captures b3.
        val after = state.play(p("b2")).state
        assertTrue(after.isCaptured(p("b3")))
        assertFalse(after.isCaptured(p("a2")))
        assertEquals(1, after.score(Player.BLUE))
    }

    @Test
    fun `a dot joined to the border by straight steps is safe and the others are exposed`() {
        val state = GameState.fromAscii(
            """
            R.......
            RR......
            ..R.....
            ....R...
            ....RR..
            ........
            ......B.
            ........
            """,
        )

        assertEquals(setOf(p("a1"), p("a2"), p("b2")), Grounding.safe(state, Player.RED))
        val exposed = Grounding.exposed(state, Player.RED).map { it.toSet() }.toSet()
        assertEquals(setOf(setOf(p("c3")), setOf(p("e4"), p("e5"), p("f5"))), exposed)
        assertEquals(listOf(listOf(p("g7"))), Grounding.exposed(state, Player.BLUE))
    }
}
