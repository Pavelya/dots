package dots.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameStateTest {

    /** Red is one dot short of a diamond around the blue dot in the middle. */
    private val almostDiamond = """
        .....
        ..R..
        .RBR.
        .....
        .....
    """

    /** Red is one dot short of a square ring; cells inside: (2,2) (blue), (3,2), (4,2), (2,3), (3,3), (4,3). */
    private val almostRing = """
        ......
        .RRRR.
        .RB..R
        .R...R
        .RRR..
        ......
    """

    private val almostEmptyRing = almostRing.replace('B', '.')

    @Test
    fun `diamond of four dots captures the dot in the middle`() {
        val result = GameState.fromAscii(almostDiamond).play(Pos(2, 3))

        assertEquals(1, result.captures.size)
        assertEquals(Player.RED, result.captures[0].by)
        assertEquals(listOf(Pos(2, 2)), result.captures[0].dots)
        assertEquals(1, result.state.score(Player.RED))
        assertTrue(result.state.isCaptured(2, 2))
        assertTrue(result.state.isDead(2, 2))
        assertEquals(Player.BLUE, result.state.toMove)
    }

    @Test
    fun `a ring with an orthogonal gap captures nothing`() {
        val result = GameState.fromAscii(almostDiamond).play(Pos(2, 4))

        assertTrue(result.captures.isEmpty())
        assertEquals(0, result.state.score(Player.RED))
        assertFalse(result.state.isCaptured(2, 2))
    }

    @Test
    fun `square ring captures the enemy and takes the cells inside out of play`() {
        val before = GameState.fromAscii(almostRing)
        assertEquals(0, before.score(Player.RED))

        val result = before.play(Pos(4, 4))

        assertEquals(1, result.state.score(Player.RED))
        assertTrue(result.state.isDead(3, 2))
        assertTrue(result.state.isDead(2, 3))
        assertTrue(result.state.isDead(3, 3))
        assertFalse(result.state.isLegal(3, 3))
    }

    @Test
    fun `a ring around empty cells captures nothing and stays playable`() {
        val result = GameState.fromAscii(almostEmptyRing).play(Pos(4, 4))

        assertTrue(result.captures.isEmpty())
        assertTrue(result.state.isLegal(2, 2))
        assertFalse(result.state.isDead(2, 2))
    }

    @Test
    fun `a dot dropped into an already closed enemy ring is captured at once`() {
        val closed = GameState.fromAscii(almostEmptyRing).play(Pos(4, 4)).state

        val result = closed.play(Pos(2, 2))

        assertEquals(1, result.captures.size)
        assertEquals(Player.RED, result.captures[0].by)
        assertEquals(listOf(Pos(2, 2)), result.captures[0].dots)
        assertEquals(1, result.state.score(Player.RED))
        assertEquals(0, result.state.score(Player.BLUE))
    }

    @Test
    fun `dots on the border can never be enclosed`() {
        val state = GameState.fromAscii(
            """
            .....
            RR...
            B....
            RR...
            .....
            """,
        )

        val result = state.play(Pos(1, 2))

        assertTrue(result.captures.isEmpty())
        assertEquals(0, result.state.score(Player.RED))
    }

    @Test
    fun `the ringing player's own dots inside do not score`() {
        val result = GameState.fromAscii(
            """
            .....
            ..R..
            .RRR.
            .....
            ....B
            """,
        ).play(Pos(2, 3))

        assertTrue(result.captures.isEmpty())
        assertEquals(0, result.state.score(Player.RED))
    }

    @Test
    fun `one ring captures every enemy dot inside`() {
        val result = GameState.fromAscii(
            """
            ......
            ..RR..
            .RBBR.
            ..R...
            ......
            """,
        ).play(Pos(3, 3))

        assertEquals(1, result.captures.size)
        assertEquals(setOf(Pos(2, 2), Pos(3, 2)), result.captures[0].dots.toSet())
        assertEquals(2, result.state.score(Player.RED))
    }

    @Test
    fun `a bigger ring takes the rim of an earlier capture while captured dots stay captured`() {
        // A red diamond (one dot short) around a blue dot, all inside a blue ring (one dot short).
        val start = GameState.start(
            width = 9,
            height = 9,
            dots = mapOf(
                Pos(4, 3) to Player.RED, Pos(3, 4) to Player.RED, Pos(5, 4) to Player.RED,
                Pos(4, 4) to Player.BLUE,
                Pos(4, 1) to Player.BLUE, Pos(5, 2) to Player.BLUE, Pos(6, 3) to Player.BLUE,
                Pos(7, 4) to Player.BLUE, Pos(6, 5) to Player.BLUE, Pos(5, 6) to Player.BLUE,
                Pos(4, 7) to Player.BLUE, Pos(3, 6) to Player.BLUE, Pos(2, 5) to Player.BLUE,
                Pos(1, 4) to Player.BLUE, Pos(2, 3) to Player.BLUE,
            ),
        )

        val afterRed = start.play(Pos(4, 5)).state
        assertEquals(1, afterRed.score(Player.RED))
        assertTrue(afterRed.isCaptured(4, 4))

        val afterBlue = afterRed.play(Pos(3, 2)).state
        assertEquals(4, afterBlue.score(Player.BLUE))
        assertEquals(1, afterBlue.score(Player.RED))
        assertTrue(afterBlue.isCaptured(4, 3))
        assertTrue(afterBlue.isCaptured(4, 5))
        assertTrue(afterBlue.isCaptured(4, 4))
    }

    @Test
    fun `moves into dead cells, onto dots and off the board are illegal`() {
        val state = GameState.fromAscii(almostDiamond).play(Pos(2, 3)).state

        assertFalse(state.isLegal(2, 2))
        assertFailsWith<IllegalArgumentException> { state.play(Pos(2, 2)) }
        assertFailsWith<IllegalArgumentException> { state.play(Pos(1, 2)) }
        assertFailsWith<IllegalArgumentException> { state.play(Pos(9, 9)) }
    }

    @Test
    fun `a starting position that already contains a capture is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            GameState.fromAscii(
                """
                .....
                ..R..
                .RBR.
                ..R..
                .....
                """,
            )
        }
    }

    @Test
    fun `a dot budget ends the game after everyone has placed theirs`() {
        var state = GameState.start(5, 5, dotsPerPlayer = 2)
        val moves = listOf(Pos(0, 0), Pos(4, 4), Pos(0, 1), Pos(4, 3))
        for ((n, move) in moves.withIndex()) {
            assertFalse(state.isOver, "game over too early after $n moves")
            state = state.play(move).state
        }

        assertTrue(state.isOver)
        assertNull(state.winner)
        assertEquals(0, state.dotsLeft(Player.RED))
        assertTrue(state.legalMoves().isEmpty())
    }

    @Test
    fun `winner is the player with more captured dots`() {
        val state = GameState.fromAscii(almostDiamond, dotsPerPlayer = 1)
            .play(Pos(2, 3)).state
            .play(Pos(0, 0)).state

        assertTrue(state.isOver)
        assertEquals(Player.RED, state.winner)
    }

    @Test
    fun `two passes in a row end the game`() {
        val state = GameState.start(5, 5)

        assertFalse(state.pass().isOver)
        assertTrue(state.pass().pass().isOver)
        assertFalse(state.pass().play(Pos(0, 0)).state.pass().isOver)
    }

    @Test
    fun `states are immutable`() {
        val before = GameState.start(5, 5)
        val after = before.play(Pos(2, 2)).state

        assertNull(before.ownerAt(2, 2))
        assertEquals(Player.RED, after.ownerAt(2, 2))
        assertEquals(Player.RED, before.toMove)
        assertEquals(Player.BLUE, after.toMove)
    }

    @Test
    fun `ascii output marks captured dots and dead cells`() {
        val state = GameState.fromAscii(almostRing).play(Pos(4, 4)).state

        assertEquals(
            """
            ......
            .RRRR.
            .Rb,,R
            .R,,,R
            .RRRR.
            ......
            """.trimIndent(),
            state.toAscii(),
        )
    }
}
