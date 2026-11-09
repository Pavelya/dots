package dots.campaign

import dots.engine.GameState
import dots.engine.Notation
import dots.engine.Player
import kotlin.test.Test
import kotlin.test.assertEquals

class WallCostTest {

    private val walls = WallCost()

    private fun index(state: GameState, point: String): Int = Notation.parse(point)!!.let { it.y * state.width + it.x }

    private fun capture(map: String, attacker: Player, point: String, limit: Int = 10): Int {
        val state = GameState.fromAscii(map)
        return walls.toCapture(state, attacker, index(state, point), limit)
    }

    @Test
    fun `a lone dot needs a diamond of four`() {
        val map = """
            .......
            .......
            .......
            ...B...
            .......
            .......
            .......
        """
        assertEquals(4, capture(map, Player.RED, "d4"))
        assertEquals(3, capture(map, Player.RED, "d4", limit = 2), "more than the limit gives limit + 1")
    }

    @Test
    fun `walls already in place count for nothing`() {
        val map = """
            .....
            ..R..
            .RB..
            .....
            .....
        """
        assertEquals(2, capture(map, Player.RED, "c3"))
    }

    @Test
    fun `a dot on the border can never be captured`() {
        assertEquals(Int.MAX_VALUE, capture(".....\n.....\nB....\n.....\n.....", Player.RED, "a3"))
    }

    @Test
    fun `a group joined to the border only diagonally can be captured`() {
        // G-19: b3 touches a2 diagonally, and a2 is on the border, yet blue captures b3 at b2.
        val map = """
            .....
            R.B..
            BRB..
            .B...
            .....
        """
        assertEquals(1, capture(map, Player.BLUE, "b3"))
    }

    @Test
    fun `the ring goes round the enemy's dots, which cannot be built on`() {
        // c3 is joined to b3, so the ring must hold b3 too: a3, b2 and b4 close it.
        val map = """
            .......
            ..RRR..
            .BBBR..
            ..RRR..
            .......
        """
        assertEquals(3, capture(map, Player.RED, "c3"))
    }

    @Test
    fun `saving a dot counts the empty points of its shortest straight chain`() {
        val state = GameState.fromAscii(
            """
            .......
            .......
            ..R....
            ..R....
            .......
            .......
            .......
            """,
        )
        assertEquals(2, walls.toSave(state, Player.RED, index(state, "c4")))

        val blocked = GameState.fromAscii(
            """
            ..B....
            ..B....
            BBRB...
            ..R....
            .......
            .......
            .......
            """,
        )
        // Up, left and right are blue; down through c4, then left: b4 and a4.
        assertEquals(2, walls.toSave(blocked, Player.RED, index(blocked, "c3")))
    }
}
