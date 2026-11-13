package dots.campaign

import dots.ai.AiConfig
import dots.ai.Hints
import dots.ai.SearchAi
import dots.engine.Booster
import dots.engine.GameState
import dots.engine.Notation
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

/**
 * The Hint booster in a level shows the move that serves the player's mission ([Level.hint]). For
 * each of the six missions, a hand-made position where that move is not the one the hint that
 * plays only for captures ([Hints.hint]) shows. The player is red and moves first; each position is
 * the level's start, or the position after the moves given, with red to move.
 */
class LevelHintTest {

    private val seeds = 1..3

    private fun level(mission: Mission, map: List<String>, dots: Int = 10) =
        Level(1, 1, "Test", Strength.HARD, dots, LevelMap(map), mission, boosters = mapOf(Booster.HINT to 1))

    private fun position(level: Level, vararg moves: String): GameState =
        moves.fold(level.newGame()) { state, move -> state.play(Notation.parse(move)!!).state }

    /** The level's hint shows [goalMove], and the capture-only hint shows [captureMove]. */
    private fun assertHint(level: Level, state: GameState, goalMove: String, captureMove: String) {
        val captures = seeds.map { Hints.hint(state, SearchAi(AiConfig.HINT, Random(it)))?.pos?.let(Notation::name) }.toSet()
        val hints = seeds.map { level.hint(state, Random(it))?.pos?.let(Notation::name) }.toSet()
        assertEquals(setOf(captureMove), captures, "the capture-only hint")
        assertEquals(setOf(goalMove), hints, "the hint for '${level.mission.text}'")
    }

    /** The mission's outcome once red has played [move] in [state] and the level's computer has replied. */
    private fun outcomeAfter(level: Level, state: GameState, move: String): Set<Outcome> = seeds.map { seed ->
        val after = state.play(Notation.parse(move)!!).state
        level.status(after.play(level.computer(Random(seed)).chooseMove(after)!!).state).outcome
    }.toSet()

    /**
     * The blue dot at c3 is one red dot from capture, at c4; so are the three at f6, g6 and h6, at
     * h7.
     */
    private val oneOrThree = listOf(
        "...........",
        "..R........",
        ".RBR.......",
        "...........",
        ".....RRR...",
        "....RBBBR..",
        ".....RR....",
        "...........",
        "...........",
    )

    @Test
    fun `target - the hint captures the target, not three other dots`() {
        val level = level(Mission.Target(Notation.parse("c3")!!), oneOrThree)
        assertHint(level, level.newGame(), goalMove = "c4", captureMove = "h7")
    }

    /**
     * The red dot at c3 is one blue dot from capture, at c4. Red can capture the three blue dots
     * at f6, g6 and h6 with h7.
     */
    private val saveOneOrTakeThree = listOf(
        "...........",
        "..B........",
        ".BRB.......",
        "...........",
        ".....RRR...",
        "....RBBBR..",
        ".....RR....",
        "...........",
        "...........",
    )

    @Test
    fun `rescue - the hint saves the dot to rescue, rather than capture three`() {
        val level = level(Mission.Rescue(Notation.parse("c3")!!), saveOneOrTakeThree)
        assertHint(level, level.newGame(), goalMove = "c4", captureMove = "h7")
    }

    @Test
    fun `survive - the hint saves the dot about to fall, rather than capture three`() {
        val level = level(Mission.Survive(3), saveOneOrTakeThree)
        assertHint(level, level.newGame(), goalMove = "c4", captureMove = "h7")
    }

    /**
     * Red can capture the blue dot at c3 with c4. The two red dots at g6 and h6 are one blue dot
     * from capture, at h7.
     */
    private val takeOneOrSaveTwo = listOf(
        "...........",
        "..R........",
        ".RBR.......",
        "...........",
        "......BB...",
        ".....BRRB..",
        "......B....",
        "...........",
        "...........",
    )

    @Test
    fun `capture - the hint captures the dot the mission asks for, rather than save two`() {
        val level = level(Mission.Capture(1, 3), takeOneOrSaveTwo)
        assertHint(level, level.newGame(), goalMove = "c4", captureMove = "h7")
    }

    /**
     * Red can capture the blue dot at b5 with c5. Blue threatens e2, which joins e3 and e4 to the
     * border at e1, so that no blue dot could be captured any more.
     */
    private val captureOrBlock = listOf(
        "....B....",
        ".........",
        "....B....",
        "RR..B....",
        "RB.......",
        "RR.......",
        ".........",
        ".........",
        ".........",
    )

    @Test
    fun `win by - the hint keeps the margin in reach rather than capture a dot`() {
        // Each side has 2 dots. After c5 and e2, red has 1 point and could add at most blue's last
        // dot: 2, less than 3. After e2, the three blue dots can still be captured.
        val level = level(Mission.WinBy(3), captureOrBlock, dots = 2)
        val start = level.newGame()
        assertHint(level, start, goalMove = "e2", captureMove = "c5")
        assertEquals(setOf(Outcome.FAILED), outcomeAfter(level, start, "c5"), "the computer answers c5 with e2")
        assertEquals(setOf(Outcome.PLAYING), outcomeAfter(level, start, "e2"))
    }

    /**
     * As [captureOrBlock], and blue can capture the three red dots at h6, h7 and h8 with g7. Every
     * other blue dot is joined to the border.
     */
    private val behindThree = listOf(
        "....B....",
        ".........",
        "....B....",
        "RR..B....",
        "RB.....BB",
        "RR....BRB",
        ".......RB",
        "......BRB",
        "......BB.",
    )

    @Test
    fun `win with fewer dots - the hint keeps the win in reach rather than capture a dot`() {
        // Red played a9 and blue took three red dots with g7. Red has 2 dots left, blue 3. After c5
        // and e2, red has 1 point and could add at most blue's 2 dots left: 3, no more than the 3
        // it lost. After e2, the three blue dots can still be captured.
        val level = level(Mission.WinWithFewerDots(3), behindThree, dots = 4)
        val state = position(level, "a9", "g7")
        assertHint(level, state, goalMove = "e2", captureMove = "c5")
        assertEquals(setOf(Outcome.FAILED), outcomeAfter(level, state, "c5"), "the computer answers c5 with e2")
        assertEquals(setOf(Outcome.PLAYING), outcomeAfter(level, state, "e2"))
    }

    @Test
    fun `at the start of every campaign puzzle the hint is a first move that passes`() {
        // Without the solver, the hint misses in about one level in six. Its time cap is lifted so
        // that only its position budget limits it, the same on every machine.
        val solver = Solver()
        val missed = Campaign.levels.filter { solver.isPuzzle(it.mission) && it.rule?.turnRule == null }.mapNotNull { base ->
            val level = base.copy(boosters = base.boosters + (Booster.HINT to 1))
            val hint = LevelHint.find(level, level.newGame(), Random(1), solverTime = 1.minutes)!!.pos
            val passing = solver.solve(level).winningFirstMoves
            if (hint in passing) null else "${level.code}: ${Notation.name(hint)} is not one of ${passing.map(Notation::name)}"
        }
        assertEquals(emptyList(), missed)
    }

    @Test
    fun `the player's goal is the computer's seen from the other side`() {
        val capture = level(Mission.Capture(1, 3), takeOneOrSaveTwo)
        assertEquals(capture.human, capture.playerGoal.player)
        assertEquals(listOf(capture.goal.losses, capture.goal.gains), listOf(capture.playerGoal.gains, capture.playerGoal.losses))
        assertEquals(true, capture.playerGoal.reached(position(capture, "c4")), "red took a dot")
        assertEquals(false, capture.goal.reached(position(capture, "c4")))

        val survive = level(Mission.Survive(3), saveOneOrTakeThree)
        assertEquals(listOf(50, 200), listOf(survive.playerGoal.gains, survive.playerGoal.losses))
        assertNull(survive.playerGoal.reached(position(survive, "c4")))
        assertEquals(false, survive.playerGoal.reached(position(survive, "h7", "c4")), "blue took a dot")

        val rescue = level(Mission.Rescue(Notation.parse("c3")!!), saveOneOrTakeThree)
        assertEquals(rescue.goal.marked, rescue.playerGoal.marked)
    }

    @Test
    fun `no hint once the level's hint is used, or in a level without one`() {
        val level = level(Mission.Capture(1, 3), takeOneOrSaveTwo)
        val start = level.newGame()
        val hinted = start.apply(level.hint(start)!!).state
        assertNull(level.hint(hinted), "one booster per turn")
        val nextTurn = hinted.play(Notation.parse("k9")!!).state.play(Notation.parse("k1")!!).state
        assertNull(level.hint(nextTurn), "the level's one hint is used")
        val noHint = level.copy(boosters = emptyMap())
        assertNull(noHint.hint(noHint.newGame()))
    }
}
