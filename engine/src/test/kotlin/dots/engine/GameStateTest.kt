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

    /** Red has closed an empty ring and blue has dropped a dot into it at (2,2); red to move. */
    private fun intruderInsideRing(): GameState =
        GameState.fromAscii(almostEmptyRing).play(Pos(4, 4)).state.play(Pos(2, 2)).state

    @Test
    fun `a dot dropped into an empty enemy ring stays alive`() {
        val state = intruderInsideRing()

        assertEquals(0, state.score(Player.RED))
        assertFalse(state.isCaptured(2, 2))
    }

    @Test
    fun `a move elsewhere does not capture the dot inside the ring`() {
        val result = intruderInsideRing().play(Pos(0, 5))

        assertTrue(result.captures.isEmpty())
        assertFalse(result.state.isCaptured(2, 2))
    }

    @Test
    fun `the ring owner captures the intruder by closing a new chain around it`() {
        // Red drops a wall across the middle of the ring; the second dot closes the left half.
        val state = intruderInsideRing()
            .play(Pos(3, 2)).state
            .play(Pos(0, 5)).state

        val result = state.play(Pos(3, 3))

        assertEquals(1, result.captures.size)
        assertEquals(listOf(Pos(2, 2)), result.captures[0].dots)
        assertEquals(1, result.state.score(Player.RED))
        assertTrue(result.state.isDead(2, 3))
        assertTrue(result.state.isLegal(4, 2), "the half without an enemy dot stays playable")
    }

    @Test
    fun `the first dot of a wall across the ring captures nothing`() {
        val result = intruderInsideRing().play(Pos(3, 2))

        assertTrue(result.captures.isEmpty())
    }

    @Test
    fun `dots inside an enemy ring can still close a chain of their own`() {
        // Red's ring is closed and holds one red dot. Blue builds a diamond around that dot using
        // only cells inside the ring; red plays harmlessly in the corners meanwhile.
        val state = GameState.fromAscii(
            """
            ........
            .RRRRRR.
            .R....R.
            .R.R..R.
            .R....R.
            .R....R.
            .RRRRRR.
            ........
            """,
            first = Player.BLUE,
        )
        val moves = listOf(Pos(3, 2), Pos(0, 0), Pos(2, 3), Pos(0, 7), Pos(4, 3), Pos(7, 0))
        val ready = moves.fold(state) { s, m -> s.play(m).state }
        assertEquals(0, ready.score(Player.BLUE))

        val result = ready.play(Pos(3, 4))

        assertEquals(1, result.captures.size)
        assertEquals(Player.BLUE, result.captures[0].by)
        assertEquals(listOf(Pos(3, 3)), result.captures[0].dots)
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

    @Test
    fun `stopping hands the opponent unlimited free moves`() {
        val state = GameState.fromAscii(
            """
            .......
            .......
            .......
            ...B...
            .......
            .......
            .......
            """,
            dotsPerPlayer = 1,
            first = Player.BLUE,
        ).stop()

        assertTrue(state.isCleanup)
        assertEquals(Player.RED, state.cleaner)
        assertEquals(Player.BLUE, state.stopper)
        assertEquals(Player.RED, state.toMove)

        // Four dots around the lone blue dot, though red's budget was one: the budget is waived.
        val end = listOf(Pos(3, 2), Pos(2, 3), Pos(4, 3), Pos(3, 4)).fold(state) { s, m ->
            assertEquals(Player.RED, s.toMove, "the turn must not pass to the player who stopped")
            s.play(m).state
        }
        assertEquals(1, end.score(Player.RED))
        assertFalse(end.isOver)

        val finished = end.finish()
        assertTrue(finished.isOver)
        assertEquals(Player.RED, finished.winner)
    }

    @Test
    fun `stop, pass and finish are only allowed at the right time`() {
        val playing = GameState.start(5, 5)
        assertFailsWith<IllegalStateException> { playing.finish() }

        val cleanup = playing.stop()
        assertFailsWith<IllegalStateException> { cleanup.stop() }
        assertFailsWith<IllegalStateException> { cleanup.pass() }

        val over = cleanup.finish()
        assertFailsWith<IllegalStateException> { over.finish() }
        assertFailsWith<IllegalStateException> { over.stop() }
    }

    @Test
    fun `groups touching the border are grounded and the rest are exposed`() {
        val state = GameState.fromAscii(
            """
            R.......
            .R......
            ........
            ....R...
            ....RR..
            ........
            ......B.
            ........
            """,
        )

        val exposed = Grounding.ungrounded(state, Player.RED)

        assertEquals(1, exposed.size)
        assertEquals(setOf(Pos(4, 3), Pos(4, 4), Pos(5, 4)), exposed[0].toSet())
        assertEquals(1, Grounding.ungrounded(state, Player.BLUE).size)
    }

    @Test
    fun `a cross is a 2x2 block of alternating colours in the middle`() {
        val state = GameState.start(8, 8, dots = Openings.cross(8, 8))

        assertEquals(Player.RED, state.ownerAt(3, 3))
        assertEquals(Player.BLUE, state.ownerAt(4, 3))
        assertEquals(Player.BLUE, state.ownerAt(3, 4))
        assertEquals(Player.RED, state.ownerAt(4, 4))
    }

    @Test
    fun `double cross and random crosses give each side the same number of dots`() {
        val double = Openings.doubleCross(12, 12)
        assertEquals(8, double.size)
        assertEquals(4, double.values.count { it == Player.RED })

        for (seed in 1..20) {
            val crosses = Openings.randomCrosses(20, 20, 4, kotlin.random.Random(seed))
            assertEquals(16, crosses.size, "seed $seed")
            assertEquals(8, crosses.values.count { it == Player.RED }, "seed $seed")
            GameState.start(20, 20, dots = crosses)
        }
    }

    @Test
    fun `an enclosed area remembers who enclosed it, and a bigger ring takes it over`() {
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
        assertNull(start.enclosedBy(4, 4))

        val afterRed = start.play(Pos(4, 5)).state
        assertEquals(Player.RED, afterRed.enclosedBy(4, 4))
        assertNull(afterRed.enclosedBy(0, 0))

        val afterBlue = afterRed.play(Pos(3, 2)).state
        assertEquals(Player.BLUE, afterBlue.enclosedBy(4, 4))
        assertEquals(Player.BLUE, afterBlue.enclosedBy(4, 3))
    }
}
