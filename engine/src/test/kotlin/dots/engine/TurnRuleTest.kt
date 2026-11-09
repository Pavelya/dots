package dots.engine

import dots.engine.Player.BLUE
import dots.engine.Player.RED
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TurnRuleTest {

    /** A game where blue has [rule]; [dots] each, or no limit. */
    private fun game(
        rule: TurnRule,
        dots: Int? = null,
        width: Int = 15,
        height: Int = 15,
        first: Player = RED,
        start: Map<Pos, Player> = emptyMap(),
    ) = GameState.start(width, height, dots, first, start, turnRules = mapOf(BLUE to rule))

    /**
     * The first free point for the player to move in their own rows: red fills the even rows,
     * blue the odd ones. Every area then still reaches the border, so no ring ever closes.
     */
    private fun GameState.quietMove(): Pos {
        val parity = if (toMove == RED) 0 else 1
        return (0 until height).filter { it % 2 == parity }
            .flatMap { y -> (0 until width).map { x -> Pos(x, y) } }
            .first { isLegal(it) }
    }

    /**
     * Plays quiet moves until the game is over or blue has finished [turns] turns, red passing on
     * its turns in [redPasses] (counted from 1). Returns the dots blue placed on each turn.
     */
    private fun blueTurns(start: GameState, turns: Int = Int.MAX_VALUE, redPasses: Set<Int> = emptySet()): List<Int> {
        val placed = ArrayList<Int>()
        var state = start
        var redTurn = 0
        while (!state.isOver && placed.size < turns) {
            if (state.toMove == RED) {
                redTurn++
                state = if (redTurn in redPasses) state.pass() else state.play(state.quietMove()).state
            } else {
                var dots = 0
                while (state.toMove == BLUE && !state.isOver) {
                    state = state.play(state.quietMove()).state
                    dots++
                }
                placed += dots
            }
        }
        return placed
    }

    private fun expected(every: Int, turns: Int) = List(turns) { if ((it + 1) % every == 0) 2 else 1 }

    // Double move

    @Test
    fun `a double move comes exactly on every Nth turn of blue's and never otherwise`() {
        for (every in listOf(2, 3, 4, 5, 7)) {
            assertEquals(expected(every, 40), blueTurns(game(TurnRule.DoubleMove(every)), 40), "every $every")
        }
    }

    @Test
    fun `red's passes do not move blue's schedule`() {
        assertEquals(expected(3, 12), blueTurns(game(TurnRule.DoubleMove(3)), 12, redPasses = setOf(1, 2, 5, 6, 9)))
    }

    @Test
    fun `blue's turns are counted when blue moves first too`() {
        val start = game(TurnRule.DoubleMove(2), first = BLUE)

        assertEquals(1, start.dotsThisTurn)
        assertEquals(expected(2, 10), blueTurns(start, 10))
    }

    @Test
    fun `turns blue takes in a row, once red is out of dots, each count`() {
        // Red has 2 dots, blue 9: after red's second dot blue takes every turn.
        var state = GameState.start(15, 15, 9, dotsFor = mapOf(RED to 2), turnRules = mapOf(BLUE to TurnRule.DoubleMove(3)))
        val perTurn = ArrayList<Int>()
        while (!state.isOver) {
            if (state.toMove == RED) {
                state = state.play(state.quietMove()).state
                continue
            }
            // Blue moves on and on, so the turns show only in the dots each one gives.
            val dots = state.dotsThisTurn
            state = state.play(state.quietMove()).state
            if (dots == 2) {
                assertEquals(1, state.dotsThisTurn)
                assertEquals(0, state.doubleMoveIn(BLUE))
                state = state.play(state.quietMove()).state
            }
            perTurn += dots
        }
        // Turns 1 to 7 take 1, 1, 2, 1, 1, 2, 1 dots: 9 in all.
        assertEquals(listOf(1, 1, 2, 1, 1, 2, 1), perTurn)
    }

    @Test
    fun `with one dot left, the double move is a single dot`() {
        // 7 dots each: blue's turns take 1, 1, 2, 1, 1 and then 1, its last dot, on turn 6.
        assertEquals(listOf(1, 1, 2, 1, 1, 1), blueTurns(game(TurnRule.DoubleMove(3), dots = 7)))
    }

    @Test
    fun `blue may pass at the start of a double move, which uses up the turn`() {
        var state = game(TurnRule.DoubleMove(2))
        state = state.play(state.quietMove()).state // red
        state = state.play(state.quietMove()).state // blue, turn 1
        state = state.play(state.quietMove()).state // red
        assertEquals(BLUE, state.toMove)
        assertEquals(2, state.dotsThisTurn)
        assertTrue(state.isLegal(Action.Pass))
        assertTrue(state.isLegal(Action.Stop))

        state = state.pass()
        assertEquals(RED, state.toMove)
        assertEquals(2, state.doubleMoveIn(BLUE)) // turn 3 is single, turn 4 the next double move
    }

    @Test
    fun `no pass or stop between the two dots of a double move`() {
        var state = game(TurnRule.DoubleMove(2))
        repeat(3) { state = state.play(state.quietMove()).state }
        state = state.play(state.quietMove()).state // blue's first dot of turn 2

        assertEquals(BLUE, state.toMove)
        assertEquals(1, state.dotsThisTurn)
        assertFalse(state.isLegal(Action.Pass))
        assertFalse(state.isLegal(Action.Stop))
        assertFailsWith<IllegalStateException> { state.pass() }
    }

    @Test
    fun `the countdown says how many of blue's turns until its double move`() {
        var state = game(TurnRule.DoubleMove(3))
        val seen = ArrayList<Pair<Player, Int?>>()
        repeat(16) {
            seen += state.toMove to state.doubleMoveIn(BLUE)
            state = state.play(state.quietMove()).state
        }
        // Red's turns show 3, 2, 1; during the double move (both dots) 0; then 3 again.
        val expected = listOf<Pair<Player, Int?>>(
            RED to 3, BLUE to 2, // turn 1 of blue's: one dot, the double move is 2 turns later
            RED to 2, BLUE to 1,
            RED to 1, BLUE to 0, BLUE to 0, // turn 3: the double move
            RED to 3, BLUE to 2,
            RED to 2, BLUE to 1,
            RED to 1, BLUE to 0, BLUE to 0,
            RED to 3, BLUE to 2,
        )
        assertEquals(expected, seen)
        assertNull(state.doubleMoveIn(RED))
    }

    @Test
    fun `the countdown stops when blue will not have two dots for its double move`() {
        // 7 dots each, double every 3: turn 3 is the last that gets two.
        var state = game(TurnRule.DoubleMove(3), dots = 7)
        val onRedTurns = ArrayList<Int?>()
        while (!state.isOver) {
            if (state.toMove == RED) onRedTurns += state.doubleMoveIn(BLUE)
            state = state.play(state.quietMove()).state
        }
        assertEquals(listOf(3, 2, 1, null, null, null, null), onRedTurns)
        assertNull(state.doubleMoveIn(BLUE))
    }

    @Test
    fun `no countdown while collecting after a stop`() {
        var state = game(TurnRule.DoubleMove(2))
        state = state.stop() // red stops: blue collects, one dot at a time

        assertTrue(state.isCleanup)
        assertNull(state.doubleMoveIn(BLUE))
        repeat(4) {
            assertEquals(1, state.dotsThisTurn)
            state = state.play(state.quietMove()).state
            assertEquals(BLUE, state.toMove)
        }
    }

    @Test
    fun `a double dot booster is not allowed on a turn that already has two dots`() {
        var state = GameState.start(
            15, 15, first = BLUE, boosters = mapOf(BLUE to mapOf(Booster.DOUBLE_DOT to 2)),
            turnRules = mapOf(BLUE to TurnRule.DoubleMove(2)),
        )
        assertTrue(state.canUse(Booster.DOUBLE_DOT)) // blue's turn 1: one dot
        state = state.play(state.quietMove()).state
        state = state.play(state.quietMove()).state // red

        assertEquals(2, state.dotsThisTurn) // blue's turn 2: the double move
        assertFalse(state.canUse(Booster.DOUBLE_DOT))
        assertFalse(state.isLegal(Action.DoubleDot))
    }

    // Two halves

    @Test
    fun `two halves - every turn two dots, one in each half`() {
        var state = game(TurnRule.TwoHalves, width = 14, height = 14)
        repeat(20) {
            state = state.play(state.quietMove()).state // red
            assertEquals(BLUE, state.toMove)
            assertEquals(2, state.dotsThisTurn)
            assertEquals(0, state.doubleMoveIn(BLUE))
            val first = state.quietMove()
            state = state.play(first).state
            assertEquals(BLUE, state.toMove)
            val second = state.quietMove()
            state = state.play(second).state
            assertEquals(RED, state.toMove)
            assertEquals(1, state.doubleMoveIn(BLUE))
            assertTrue((first.x < 7) != (second.x < 7), "$first and $second are in the same half")
        }
    }

    @Test
    fun `two halves - the second dot may not go in the first dot's half`() {
        var state = game(TurnRule.TwoHalves, width = 14, height = 14, first = BLUE)
        state = state.play(Pos(10, 5)).state // the right half first

        val legal = state.legalMoves()
        assertTrue(legal.isNotEmpty())
        assertTrue(legal.all { it.x < 7 })
        assertFalse(state.isLegal(Pos(11, 5)))
        assertTrue(state.isLegal(Pos(6, 5)))
        assertFailsWith<IllegalArgumentException> { state.play(Pos(7, 5)) }

        state = state.play(Pos(6, 5)).state
        assertEquals(RED, state.toMove)
        assertTrue(state.isLegal(Pos(11, 5)))
    }

    @Test
    fun `two halves - when the other half is full the turn ends after one dot`() {
        // Red fills the right half (columns c and d); blue moves first.
        val right = (0 until 3).flatMap { y -> listOf(Pos(2, y) to RED, Pos(3, y) to RED) }.toMap()
        val start = game(TurnRule.TwoHalves, width = 4, height = 3, first = BLUE, start = right)

        assertEquals(2, start.dotsThisTurn)
        val after = start.play(Pos(0, 0)).state
        assertEquals(RED, after.toMove)
    }

    @Test
    fun `two halves need a board of even width`() {
        val error = assertFailsWith<IllegalArgumentException> { game(TurnRule.TwoHalves, width = 15) }
        assertEquals("two halves need a board of even width", error.message)
    }

    @Test
    fun `a double move needs at least every second turn`() {
        assertFailsWith<IllegalArgumentException> { TurnRule.DoubleMove(1) }
    }
}
