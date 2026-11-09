package dots.campaign

import dots.ai.AiConfig
import dots.ai.SearchAi
import dots.engine.Action
import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BossLevelTest {

    /** A boss level file with [rule]: a 10x10 board, the computer's fortress of 8 dots top left. */
    private fun file(rule: String, dots: Int = 20, computer: String = "normal") = """
        world: 4
        level: 20
        name: Ink Bottle
        computer: $computer
        dots: $dots
        mission: win
        solutions: open
        boosters: none
        rule: $rule
        stars: none
        map:
        ..........
        ..........
        ...BB.....
        ..B..B....
        ..B..B....
        ...BB.....
        ..........
        ......RB..
        ......BR..
        ..........
    """.trimIndent() + "\n"

    private fun level(rule: String, dots: Int = 20, computer: String = "normal") =
        LevelFormat.parse(file(rule, dots, computer), "4-20.level")

    /** One of the computer's turns: the dots it placed, in order. */
    private class Turn(val dotsLeftBefore: Int, val dots: List<Pos>)

    /**
     * Plays [level] to the end, both sides the computer at its level's strength, and returns the
     * computer's turns. Every action is checked to be legal, and the game must replay from its
     * record to the same end.
     */
    private fun computerTurns(level: Level, seed: Int): List<Turn> {
        val human = SearchAi(AiConfig.NORMAL, Random(seed))
        val computer = SearchAi(level.ai, Random(seed + 100))
        var state = level.newGame()
        val actions = ArrayList<Action>()
        val turns = ArrayList<Turn>()
        while (!state.isOver) {
            if (state.toMove == level.human) {
                val action = human.choose(state)
                assertTrue(state.isLegal(action), "$action")
                actions += action
                state = state.apply(action).state
                continue
            }
            // The human always has dots left while the computer moves, so its turn ends when the
            // human is to move again.
            val dotsLeft = state.dotsLeft(level.computer)!!
            val dots = ArrayList<Pos>()
            while (state.toMove == level.computer && !state.isOver) {
                val action = computer.choose(state)
                assertTrue(state.isLegal(action), "$action")
                actions += action
                if (action is Action.Place) dots += action.pos
                state = state.apply(action).state
            }
            turns += Turn(dotsLeft, dots)
        }
        assertEquals(state.toAscii(), LevelRecord(level, actions).replay().toAscii())
        return turns
    }

    @Test
    fun `every rule is read from a level file and written back the same`() {
        val rules = mapOf(
            "double move every 4 turns" to SpecialRule.DoubleMove(4),
            "fortress of 8 dots" to SpecialRule.Fortress(8),
            "two groups at once" to SpecialRule.TwoGroups,
        )
        for ((text, rule) in rules) {
            val level = level(text)
            assertEquals(rule, level.rule)
            assertEquals(text, rule.text)
            assertEquals(file(text), LevelFormat.write(level))
            assertNull(level.unplayable)
        }
    }

    @Test
    fun `the computer moves twice exactly on schedule, and never otherwise`() {
        for (seed in 1..3) {
            val level = level("double move every 3 turns")
            val turns = computerTurns(level, seed)

            assertTrue(turns.size >= 9, "only ${turns.size} turns")
            for ((i, turn) in turns.withIndex()) {
                val double = (i + 1) % 3 == 0 && turn.dotsLeftBefore >= 2
                assertEquals(if (double) 2 else 1, turn.dots.size, "turn ${i + 1}, seed $seed")
            }
        }
    }

    @Test
    fun `the countdown follows the computer's turns`() {
        val level = level("double move every 3 turns")
        var state = level.newGame()
        val countdown = ArrayList<Int?>()
        val computer = SearchAi(level.ai, Random(1))
        repeat(7) {
            countdown += state.doubleMoveIn(level.computer)
            state = state.apply(SearchAi(AiConfig.EASY, Random(it)).choose(state)).state
            while (state.toMove == level.computer) state = state.apply(computer.choose(state)).state
        }
        assertEquals(listOf<Int?>(3, 2, 1, 3, 2, 1, 3), countdown)
    }

    @Test
    fun `with two groups the computer places a dot in each half every turn`() {
        val level = level("two groups at once")
        val turns = computerTurns(level, seed = 4)

        assertTrue(turns.size >= 5, "only ${turns.size} turns")
        for ((i, turn) in turns.withIndex()) {
            if (turn.dotsLeftBefore < 2) continue
            assertEquals(2, turn.dots.size, "turn ${i + 1}")
            val (first, second) = turn.dots
            assertTrue((first.x < 5) != (second.x < 5), "turn ${i + 1}: $first and $second are in the same half")
        }
    }

    @Test
    fun `the fortress is the computer's biggest group on the map`() {
        val level = level("fortress of 8 dots")

        assertEquals(
            listOf(Pos(3, 2), Pos(4, 2), Pos(2, 3), Pos(5, 3), Pos(2, 4), Pos(5, 4), Pos(3, 5), Pos(4, 5)),
            level.fortress,
        )
        assertEquals(8, level.headStart)
        assertTrue(level("none").fortress.isEmpty())
        // A fortress does not change the turns: the computer places one dot each.
        val turns = computerTurns(level, seed = 5)
        assertTrue(turns.all { it.dots.size == 1 })
    }

    @Test
    fun `a rule that does not fit its level names the rule's line`() {
        val cases = mapOf(
            file("fortress of 9 dots") to "line 9: the fortress has 9 dots, but the biggest group of computer dots on the map has 8",
            file("fortress of 7 dots") to "line 9: 7 is not between 8 and 9999",
            file("double move every 4 turns", dots = 4) to
                "line 9: the computer's 4 dots run out before its first double move, which needs 5",
            file("two groups at once").replace(".\n", "..\n") to
                "line 9: the rule 'two groups at once' needs a board of even width, so that its halves are equal; this one is 11 points wide",
            file("giant") to "line 9: unknown special rule 'giant' (none; double move every 5 turns; fortress of 12 dots; two groups at once)",
        )
        for ((text, expected) in cases) {
            val error = assertFailsWith<LevelFormatException>(expected) { LevelFormat.parse(text, "bad.level") }
            assertEquals("bad.level, $expected", error.message)
        }
    }

    @Test
    fun `two groups of the fortress's size are refused`() {
        val twoFortresses = LevelMap(
            listOf(
                ".BB.....BB.",
                "B..B...B..B",
                "B..B...B..B",
                ".BB.....BB.",
                "...........",
                ".....R.....",
                "...........",
            ),
        )
        val error = assertFailsWith<IllegalArgumentException> {
            Level(1, 1, "Twins", Strength.EASY, 10, twoFortresses, rule = SpecialRule.Fortress(8))
        }
        assertEquals("two groups of computer dots on the map have 8 dots; the fortress must be bigger than the others", error.message)
    }

    @Test
    fun `the puzzle checker leaves out levels where the computer places two dots`() {
        val level = level("double move every 3 turns").copy(mission = Mission.Capture(dots = 1, moves = 3))

        val error = assertFailsWith<IllegalArgumentException> { Solver().solve(level) }
        assertEquals("Level 4-20 has the rule 'double move every 3 turns', which the solver does not play", error.message)
    }

    @Test
    fun `the computer has the rule, the player does not`() {
        val state: GameState = level("double move every 2 turns").newGame()

        assertEquals(dots.engine.TurnRule.DoubleMove(2), state.turnRule(Player.BLUE))
        assertNull(state.turnRule(Player.RED))
        assertNull(state.doubleMoveIn(Player.RED))
        assertEquals(2, state.doubleMoveIn(Player.BLUE))
    }
}
