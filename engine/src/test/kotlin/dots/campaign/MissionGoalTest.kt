package dots.campaign

import dots.ai.AiConfig
import dots.ai.SearchAi
import dots.engine.GameState
import dots.engine.Notation
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The computer plays for the level's mission (`MissionGoal`). For each of the six missions, a
 * hand-made position where the move that serves the mission is not the move a computer that plays
 * only for captures makes. The human is red and moves first; each position is the map after the
 * red moves given, with blue to move.
 */
class MissionGoalTest {

    private val seeds = 1..5

    private fun level(mission: Mission, map: List<String>, dots: Int = 10) =
        Level(1, 1, "Test", Strength.HARD, dots, LevelMap(map), mission)

    private fun position(level: Level, vararg moves: String): GameState =
        moves.fold(level.newGame()) { state, move -> state.play(Notation.parse(move)!!).state }

    /** The moves [config] makes in [state] over [seeds], playing for [level]'s mission or only for captures. */
    private fun moves(level: Level, state: GameState, config: AiConfig, goal: Boolean): Set<String> = seeds.map { seed ->
        val computer = if (goal) level.computer(Random(seed), config) else SearchAi(config, Random(seed))
        Notation.name(computer.chooseMove(state)!!)
    }.toSet()

    /** HARD plays [goalMove] for the mission, and HARD playing only for captures plays [captureMove] or one of [orCapture]. */
    private fun assertGoalMove(level: Level, state: GameState, goalMove: String, captureMove: String, vararg orCapture: String) {
        val captures = moves(level, state, AiConfig.HARD, goal = false)
        assertTrue(captures.all { it == captureMove || it in orCapture }, "playing only for captures, it played $captures")
        assertEquals(setOf(goalMove), moves(level, state, AiConfig.HARD, goal = true), "playing for '${level.mission.text}'")
    }

    /**
     * Red has just played g7. The target c3 and the three blue dots at f6, g6 and h6 are each one
     * red dot from capture: c4 saves the target, h7 the three.
     */
    private val targetOrThree = listOf(
        "...........",
        "..R........",
        ".RBR.......",
        "...........",
        ".....RRR...",
        "....RBBBR..",
        ".....R.....",
        "...........",
        "...........",
    )

    @Test
    fun `target - the computer guards the target, not its bigger group`() {
        val level = level(Mission.Target(Notation.parse("c3")!!), targetOrThree)
        assertGoalMove(level, position(level, "g7"), goalMove = "c4", captureMove = "h7")
    }

    /** The red dot to rescue, c3, falls to c4; the three red dots at f6, g6 and h6 fall to h7. */
    private val rescueOrThree = listOf(
        "...........",
        "..B........",
        ".BRB.......",
        "...........",
        ".....BBB...",
        "....BRRRB..",
        ".....BB....",
        "...........",
        "...........",
    )

    @Test
    fun `rescue - the computer captures the dot to rescue, not three others`() {
        val level = level(Mission.Rescue(Notation.parse("c3")!!), rescueOrThree)
        assertGoalMove(level, position(level, "j8"), goalMove = "c4", captureMove = "h7")
    }

    /**
     * Red has just played d3: one more red dot, at c4, captures the blue dot at c3. Blue can
     * capture the two red dots at g6 and h6 with h7 instead.
     */
    private val blockOrTakeTwo = listOf(
        "...........",
        "..R........",
        ".RB........",
        "...........",
        "......BB...",
        ".....BRRB..",
        "......B....",
        "...........",
        "...........",
    )

    @Test
    fun `capture - the computer blocks the capture the mission asks for, rather than take two dots`() {
        val level = level(Mission.Capture(1, 3), blockOrTakeTwo)
        assertGoalMove(level, position(level, "d3"), goalMove = "c4", captureMove = "h7")
    }

    /**
     * Red has just played g7: one more red dot, at h7, captures the blue dots at f6, g6 and h6.
     * Blue can capture the red dot at c3 with c4 instead.
     */
    private val takeOneOrSaveThree = listOf(
        "...........",
        "..B........",
        ".BRB.......",
        "...........",
        ".....RRR...",
        "....RBBBR..",
        ".....R.....",
        "...........",
        "...........",
    )

    @Test
    fun `survive - the computer takes a dot, which fails the mission, rather than save three`() {
        val level = level(Mission.Survive(3), takeOneOrSaveThree)
        assertGoalMove(level, position(level, "g7"), goalMove = "c4", captureMove = "h7")
    }

    /**
     * Blue can capture the red dot at b5 with c5, or the one at b8 with c8. Or it can play e2, which joins e3 and e4 to the
     * border, so that no blue dot can be captured any more.
     */
    private val groundOrCapture = listOf(
        "....B....",
        ".........",
        "....B....",
        "BB..B....",
        "BR.......",
        "BB.......",
        "BB.......",
        "BR.......",
        ".B.......",
    )

    @Test
    fun `win by - the computer puts the margin out of reach rather than capture a dot`() {
        // After e2 the player can capture nothing, and blue has 1 dot left: 1 is less than 2.
        val level = level(Mission.WinBy(2), groundOrCapture, dots = 2)
        assertGoalMove(level, position(level, "h8"), goalMove = "e2", captureMove = "c5", "c8")
    }

    @Test
    fun `win with fewer dots - the computer puts the win out of reach rather than capture a dot`() {
        // Blue has taken b5 and red has placed both its dots. After e2 blue is 1 up and has 1 dot
        // left, which is all the player could still capture. Blue could take b8 with c8 instead.
        val level = level(Mission.WinWithFewerDots(2), groundOrCapture, dots = 3)
        assertGoalMove(level, position(level, "h8", "c5", "h2"), goalMove = "e2", captureMove = "c8")
    }

    @Test
    fun `a weaker step does not see a goal away from the last dots`() {
        // The position of the "win with fewer dots" test: e2 is three points from the last dots.
        val level = level(Mission.WinWithFewerDots(2), groundOrCapture, dots = 3)
        val state = position(level, "h8", "c5", "h2")
        for (config in AiConfig.STEPS.dropLast(1)) {
            assertTrue("e2" !in moves(level, state, config, goal = true), "a step below HARD found e2")
        }
        assertEquals(setOf("e2"), moves(level, state, AiConfig.HARD, goal = true))
    }

    @Test
    fun `the goal counts only on the computer's turns`() {
        val level = level(Mission.Target(Notation.parse("c3")!!), targetOrThree)
        val state = level.newGame()
        for (seed in seeds) {
            assertEquals(
                SearchAi(AiConfig.HARD, Random(seed)).chooseMove(state),
                level.computer(Random(seed), AiConfig.HARD).chooseMove(state),
            )
        }
    }

    @Test
    fun `moves left count down the mission's limit and the dots left`() {
        val capture = level(Mission.Capture(2, 4), blockOrTakeTwo, dots = 6)
        val goal = capture.goal
        val start = capture.newGame()
        val after = position(capture, "d3", "k9")
        assertEquals(4, goal.movesLeft(start, capture.human))
        assertEquals(3, goal.movesLeft(after, capture.human))
        assertEquals(5, goal.movesLeft(after, capture.computer), "the computer's dots left")

        val survive = level(Mission.Survive(3), takeOneOrSaveThree, dots = 6)
        val later = position(survive, "g7", "k9")
        assertEquals(2, survive.goal.movesLeft(later, survive.computer))
        assertEquals(5, survive.goal.movesLeft(later, survive.human), "the player's dots left")

        val target = level(Mission.Target(Notation.parse("c3")!!), targetOrThree, dots = 6)
        assertEquals(5, target.goal.movesLeft(position(target, "g7", "h7"), target.computer), "no limit but the dots left")
    }

    @Test
    fun `the goal is reached when the mission fails and lost when it passes`() {
        val survive = level(Mission.Survive(3), takeOneOrSaveThree)
        assertNull(survive.goal.reached(position(survive, "g7")))
        assertEquals(true, survive.goal.reached(position(survive, "g7", "c4")), "blue took a dot")

        val target = level(Mission.Target(Notation.parse("c3")!!), targetOrThree)
        assertEquals(false, target.goal.reached(position(target, "g7", "h7", "c4")), "red took the target")
    }
}
