package dots.campaign

import dots.engine.Action
import dots.engine.Notation
import dots.engine.Player
import dots.engine.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration

/** Known puzzles, each with its answer worked out by hand. The player is red and moves first. */
class SolverTest {

    private fun level(mission: Mission, map: List<String>, dots: Int = 6) =
        Level(1, 1, "Test", Strength.EASY, dots, LevelMap(map), mission)

    private fun points(vararg names: String): List<Pos> = names.map { Notation.parse(it)!! }

    private fun Solution.lineText(): String = line.joinToString(" ") { step ->
        val move = when (val action = step.action) {
            is Action.Place -> Notation.name(action.pos)
            else -> "pass"
        }
        if (step.player == Player.RED) move else "($move)"
    }

    @Test
    fun `capture in one, the only closing point`() {
        val level = level(
            Mission.Capture(dots = 1, moves = 1),
            listOf(
                ".....",
                "..R..",
                ".RB..",
                "..R..",
                ".....",
            ),
        )
        val solution = Solver().solve(level)

        assertEquals(Verdict.SOLVED, solution.verdict)
        assertEquals(points("d3"), solution.winningFirstMoves)
        assertEquals(1, solution.shortest)
        assertEquals("d3", solution.lineText())
    }

    @Test
    fun `capture in two, blue blocks the second closing point`() {
        // Red needs both d3 and c4; whichever it takes, blue takes the other.
        val level = level(
            Mission.Capture(dots = 1, moves = 2),
            listOf(
                ".....",
                "..R..",
                ".RB..",
                ".....",
                ".....",
            ),
        )
        val solution = Solver().solve(level)

        assertEquals(Verdict.UNSOLVABLE, solution.verdict)
        assertTrue(solution.winningFirstMoves.isEmpty())
        assertNull(solution.shortest)
    }

    @Test
    fun `capture in two, a fork that blue cannot stop`() {
        // d3 leaves both blue dots one point from capture, at c4 and e4: blue can block only one.
        val level = level(
            Mission.Capture(dots = 1, moves = 2),
            listOf(
                ".......",
                "..R.R..",
                ".RB.BR.",
                ".......",
                ".......",
            ),
        )
        val solution = Solver().solve(level)

        assertEquals(Verdict.SOLVED, solution.verdict)
        assertEquals(points("d3"), solution.winningFirstMoves)
        assertEquals(2, solution.shortest)
        val line = solution.lineText()
        assertTrue(line.startsWith("d3 (") && line.split(" ").size == 3, line)
    }

    @Test
    fun `survive one move, only blocking saves the dot`() {
        val level = level(
            Mission.Survive(1),
            listOf(
                ".....",
                "..B..",
                ".BR..",
                "..B..",
                ".....",
            ),
        )
        val solution = Solver().solve(level)

        assertEquals(Verdict.SOLVED, solution.verdict)
        assertEquals(points("d3"), solution.winningFirstMoves)
        assertEquals(1, solution.shortest)
    }

    @Test
    fun `rescue, the straight step to the edge`() {
        // a3 joins b3 to the edge in a straight line; anything else and blue captures b3 at a3.
        val level = level(
            Mission.Rescue(Pos(1, 2)),
            listOf(
                ".....",
                ".B...",
                ".RB..",
                ".B...",
                ".....",
            ),
        )
        val solution = Solver().solve(level)

        assertEquals(Verdict.SOLVED, solution.verdict)
        assertEquals(points("a3"), solution.winningFirstMoves)
        assertEquals(1, solution.shortest)
    }

    @Test
    fun `rescue, two threats make it unsolvable`() {
        // Blue captures c3 at d3 (a small ring) or at e3 (a ring around c3 and d3). Red can block one.
        val level = level(
            Mission.Rescue(Pos(2, 2)),
            listOf(
                ".......",
                "..BB...",
                ".BR....",
                "..BB...",
                ".......",
            ),
        )
        val solution = Solver().solve(level)

        assertEquals(Verdict.UNSOLVABLE, solution.verdict)
    }

    @Test
    fun `target, the closing point`() {
        val level = level(
            Mission.Target(Pos(1, 2)),
            listOf(
                ".....",
                ".R...",
                ".BR..",
                ".R...",
                ".....",
            ),
        )
        val solution = Solver().solve(level)

        assertEquals(Verdict.SOLVED, solution.verdict)
        assertEquals(points("a3"), solution.winningFirstMoves)
    }

    @Test
    fun `a puzzle with no threat at all lets every first move pass`() {
        // Blue has one dot and cannot capture anything in one move.
        val level = level(
            Mission.Survive(1),
            listOf(
                ".....",
                ".....",
                "..R..",
                ".....",
                "....B",
            ),
        )
        val solution = Solver().solve(level)

        assertEquals(Verdict.SOLVED, solution.verdict)
        assertEquals(23, solution.winningFirstMoves.size)
    }

    @Test
    fun `far points count as passing first moves when the one standing for them passes`() {
        // On 9x9, the points more than two from both dots are far: 81 points, 2 dots, 79 legal moves.
        val map = List(9) { ".".repeat(9) }.toMutableList()
        map[2] = "..R......"
        map[8] = "........B"
        val solution = Solver().solve(level(Mission.Survive(1), map))

        assertEquals(Verdict.SOLVED, solution.verdict)
        assertEquals(79, solution.winningFirstMoves.size)
        assertEquals(solution.winningFirstMoves.distinct(), solution.winningFirstMoves)
    }

    @Test
    fun `a mission longer than the search is undecided when neither side can force it`() {
        // Red's c3 needs two more dots in a line to reach the edge, and blue blocks a single way out.
        // Three moves make two ways out (c2, then b2 threatens a2 and b1); blue cannot threaten c3.
        val level = level(
            Mission.Rescue(Pos(2, 2)),
            listOf(
                ".......",
                ".......",
                "..R....",
                ".......",
                ".......",
                ".....B.",
                ".......",
            ),
        )
        assertEquals(Verdict.UNDECIDED, Solver(maxMoves = 2).solve(level).verdict)
        val solution = Solver(maxMoves = 3).solve(level)
        assertEquals(Verdict.SOLVED, solution.verdict)
        assertEquals(3, solution.shortest)
    }

    @Test
    fun `the time cap stops the search`() {
        val level = level(
            Mission.Capture(dots = 2, moves = 5),
            listOf(
                ".........",
                ".........",
                "...B.R...",
                "..R.B....",
                "...B.R...",
                "....R.B..",
                ".........",
                ".........",
                ".........",
            ),
            dots = 10,
        )
        val solution = Solver(timeLimit = Duration.ZERO).solve(level)

        assertEquals(Verdict.TIMED_OUT, solution.verdict)
    }

    @Test
    fun `whole games and long missions are not puzzles`() {
        val solver = Solver()

        assertTrue(solver.isPuzzle(Mission.Capture(dots = 2, moves = 5)))
        assertTrue(solver.isPuzzle(Mission.Survive(5)))
        assertTrue(solver.isPuzzle(Mission.Rescue(Pos(2, 2))))
        assertTrue(solver.isPuzzle(Mission.Target(Pos(2, 2))))
        assertFalse(solver.isPuzzle(Mission.Capture(dots = 2, moves = 6)))
        assertFalse(solver.isPuzzle(Mission.Survive(6)))
        assertFalse(solver.isPuzzle(Mission.Win))
        assertFalse(solver.isPuzzle(Mission.WinBy(2)))
        assertFalse(solver.isPuzzle(Mission.WinWithFewerDots(3)))
    }
}
