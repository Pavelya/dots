package dots.campaign

import dots.engine.GameState
import dots.engine.Notation
import dots.engine.Player
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Each mission on a small hand-made board. The human is red and moves first; moves alternate. */
class MissionTest {

    /** Red takes the blue dot at c3 by playing d3. */
    private val redCaptures = listOf(
        ".....",
        "..R..",
        ".RB..",
        "..R..",
        ".....",
    )

    /** Blue takes the red dot at c3 by playing d3. */
    private val blueCaptures = listOf(
        ".....",
        "..B..",
        ".BR..",
        "..B..",
        ".....",
    )

    /** Blue takes the red dot at b3 by playing a3; red saves it by playing a3 first. */
    private val redNearEdge = listOf(
        ".....",
        ".B...",
        ".RB..",
        ".B...",
        ".....",
    )

    /** Red takes the blue dot at b3 by playing a3; blue saves it by playing a3 first. */
    private val blueNearEdge = listOf(
        ".....",
        ".R...",
        ".BR..",
        ".R...",
        ".....",
    )

    private fun level(mission: Mission, dots: Int, map: List<String>) =
        Level(1, 1, "Test", Strength.EASY, dots, LevelMap(map), mission)

    private fun GameState.then(vararg moves: String): GameState =
        moves.fold(this) { state, move -> state.play(Notation.parse(move)!!).state }

    private fun assertStatus(level: Level, state: GameState, outcome: Outcome, reason: Reason?, movesLeft: Int? = null) {
        val status = level.status(state)
        assertEquals(outcome, status.outcome, "outcome in\n${state.toAscii()}")
        assertEquals(reason, status.reason, "reason in\n${state.toAscii()}")
        assertEquals(movesLeft, status.movesLeft, "moves left in\n${state.toAscii()}")
        assertEquals(outcome != Outcome.PLAYING, level.isOver(state))
        assertEquals(outcome == Outcome.PASSED, level.isPassed(state))
    }

    // Capture N in M moves

    @Test
    fun `capture passes on the move that takes the last dot it needs`() {
        val level = level(Mission.Capture(dots = 1, moves = 2), dots = 3, redCaptures)
        val start = level.newGame()
        assertStatus(level, start, Outcome.PLAYING, null, movesLeft = 2)

        val before = start.then("a1", "e5")
        assertStatus(level, before, Outcome.PLAYING, null, movesLeft = 1)

        val after = before.then("d3")
        assertStatus(level, after, Outcome.PASSED, Reason.CAPTURED_ENOUGH, movesLeft = 0)
        assertFalse(after.isOver, "the level ends before the game would")
        assertEquals(1, level.status(after).captured)
    }

    @Test
    fun `capture fails right after the last move it allows`() {
        val level = level(Mission.Capture(dots = 1, moves = 2), dots = 3, redCaptures)

        assertStatus(level, level.newGame().then("a1", "e5"), Outcome.PLAYING, null, movesLeft = 1)
        assertStatus(level, level.newGame().then("a1", "e5", "a5"), Outcome.FAILED, Reason.OUT_OF_MOVES, movesLeft = 0)
    }

    @Test
    fun `capture fails early once too few computer dots are left to take`() {
        // One blue dot can be taken, and blue's dots on the border are safe: after two of them,
        // only 1 + 1 dots are left to capture, fewer than 3.
        val level = level(Mission.Capture(dots = 3, moves = 3), dots = 3, redCaptures)

        assertStatus(level, level.newGame().then("a1", "e1"), Outcome.PLAYING, null, movesLeft = 2)
        assertStatus(level, level.newGame().then("a1", "e1", "a2", "e2"), Outcome.FAILED, Reason.OUT_OF_REACH, movesLeft = 1)
    }

    @Test
    fun `a pass is not a move`() {
        val level = level(Mission.Capture(dots = 1, moves = 1), dots = 3, redCaptures)
        val state = level.newGame().pass().then("e5")

        assertStatus(level, state, Outcome.PLAYING, null, movesLeft = 1)
    }

    // Survive M moves

    @Test
    fun `survive fails on the computer's move that captures a dot`() {
        val level = level(Mission.Survive(2), dots = 3, blueCaptures)

        assertStatus(level, level.newGame().then("a1"), Outcome.PLAYING, null, movesLeft = 2)
        val state = level.newGame().then("a1", "d3")
        assertStatus(level, state, Outcome.FAILED, Reason.LOST_A_DOT, movesLeft = 1)
        assertEquals(1, level.status(state).lost)
    }

    @Test
    fun `survive passes once the computer has placed M dots without a capture`() {
        val level = level(Mission.Survive(2), dots = 3, blueCaptures)

        assertStatus(level, level.newGame().then("d3", "e5"), Outcome.PLAYING, null, movesLeft = 1)
        assertStatus(level, level.newGame().then("d3", "e5", "a1"), Outcome.PLAYING, null, movesLeft = 1)
        assertStatus(level, level.newGame().then("d3", "e5", "a1", "e1"), Outcome.PASSED, Reason.SURVIVED, movesLeft = 0)
    }

    @Test
    fun `survive passes early once the computer can capture nothing more`() {
        // Red places both its dots on the border next to its first one: no red dot can be
        // captured any more, and none will be placed.
        val level = level(Mission.Survive(2), dots = 2, listOf("R....", ".....", "..B..", ".....", "....."))

        assertStatus(level, level.newGame().then("a2", "c4"), Outcome.PLAYING, null, movesLeft = 1)
        assertStatus(level, level.newGame().then("a2", "c4", "a3"), Outcome.PASSED, Reason.SURVIVED, movesLeft = 1)
    }

    // Rescue

    @Test
    fun `rescue passes on the move that joins the dot to the border`() {
        val level = level(Mission.Rescue(Notation.parse("b3")!!), dots = 2, redNearEdge)

        assertStatus(level, level.newGame(), Outcome.PLAYING, null)
        assertStatus(level, level.newGame().then("a3"), Outcome.PASSED, Reason.DOT_SAFE)
    }

    @Test
    fun `rescue fails on the move that captures the dot`() {
        val level = level(Mission.Rescue(Notation.parse("b3")!!), dots = 2, redNearEdge)

        assertStatus(level, level.newGame().then("e5", "a3"), Outcome.FAILED, Reason.DOT_CAPTURED)
    }

    @Test
    fun `a diagonal join to the border does not rescue the dot`() {
        val level = level(Mission.Rescue(Notation.parse("b3")!!), dots = 2, redNearEdge)

        assertStatus(level, level.newGame().then("a2"), Outcome.PLAYING, null)
        assertStatus(level, level.newGame().then("a2", "a3"), Outcome.FAILED, Reason.DOT_CAPTURED)
    }

    @Test
    fun `rescue passes when the game ends with the dot still there`() {
        val level = level(Mission.Rescue(Notation.parse("b3")!!), dots = 1, redNearEdge)
        val end = level.newGame().then("e5", "e1")

        assertTrue(end.isOver)
        assertStatus(level, end, Outcome.PASSED, Reason.GAME_OVER)
    }

    // Target

    @Test
    fun `target passes on the move that captures the target`() {
        val level = level(Mission.Target(Notation.parse("b3")!!), dots = 2, blueNearEdge)

        assertStatus(level, level.newGame(), Outcome.PLAYING, null)
        assertStatus(level, level.newGame().then("a3"), Outcome.PASSED, Reason.TARGET_CAPTURED)
    }

    @Test
    fun `target fails on the move that makes the target safe`() {
        val level = level(Mission.Target(Notation.parse("b3")!!), dots = 2, blueNearEdge)

        assertStatus(level, level.newGame().then("e5"), Outcome.PLAYING, null)
        assertStatus(level, level.newGame().then("e5", "a3"), Outcome.FAILED, Reason.TARGET_SAFE)
    }

    @Test
    fun `target fails when the game ends with the target still there`() {
        val level = level(Mission.Target(Notation.parse("b3")!!), dots = 1, blueNearEdge)

        assertStatus(level, level.newGame().then("e5", "e1"), Outcome.FAILED, Reason.GAME_OVER)
    }

    // Win, win by N, win with fewer dots

    @Test
    fun `win passes only at the end of the game, and a draw fails`() {
        val level = level(Mission.Win, dots = 1, redCaptures)

        assertStatus(level, level.newGame().then("d3"), Outcome.PLAYING, null)
        assertStatus(level, level.newGame().then("d3", "e5"), Outcome.PASSED, Reason.GAME_OVER)
        assertStatus(level, level.newGame().then("a1", "e5"), Outcome.FAILED, Reason.GAME_OVER)
    }

    @Test
    fun `win fails early once even every capturable dot would not be enough`() {
        // Every blue dot is joined to the border in a straight line, and blue plays b1 to take b2.
        // Then red is one dot down with nothing left to capture but blue's last dot.
        val map = listOf(
            "..B...",
            "BRB...",
            "BB....",
            "......",
            "....R.",
            "......",
        )
        val level = level(Mission.Win, dots = 2, map)

        assertStatus(level, level.newGame().then("f6"), Outcome.PLAYING, null)
        val state = level.newGame().then("f6", "b1")
        assertFalse(state.isOver, "red and blue each have a dot left")
        assertStatus(level, state, Outcome.FAILED, Reason.OUT_OF_REACH)
    }

    @Test
    fun `win by N needs the margin, not only more captures`() {
        val level = level(Mission.WinBy(2), dots = 2, redCaptures)

        assertStatus(level, level.newGame().then("d3", "e5"), Outcome.PLAYING, null)
        val end = level.newGame().then("d3", "e5", "a1", "e1")
        assertEquals(Player.RED, end.winner)
        assertStatus(level, end, Outcome.FAILED, Reason.GAME_OVER)
    }

    @Test
    fun `win with fewer dots gives the player a smaller budget`() {
        val level = level(Mission.WinWithFewerDots(1), dots = 2, redCaptures)
        val start = level.newGame()

        assertEquals(1, level.humanDots)
        assertEquals(1, start.dotsLeft(Player.RED))
        assertEquals(2, start.dotsLeft(Player.BLUE))

        val afterRed = start.then("d3", "e5")
        assertFalse(afterRed.isOver, "blue places its last dot alone")
        assertStatus(level, afterRed, Outcome.PLAYING, null)
        assertStatus(level, afterRed.then("e1"), Outcome.PASSED, Reason.GAME_OVER)
        assertEquals(1, afterRed.dotsPlaced(Player.RED))
        assertEquals(1, afterRed.dotsPlaced(Player.BLUE))
    }

    // Levels

    @Test
    fun `a mission decided before the first move is refused`() {
        val safe = listOf(".....", ".....", "R.B..", ".....", ".....")

        val error = runCatching { level(Mission.Rescue(Notation.parse("a3")!!), dots = 2, safe) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException && "already decided" in error.message!!, "$error")
    }
}
