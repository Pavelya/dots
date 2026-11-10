package dots.ai

import dots.engine.Action
import dots.engine.GameState
import dots.engine.Openings
import dots.engine.Player
import dots.engine.Pos
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchAiTest {

    /** One dot short of a diamond around the blue dot in the middle. */
    private val almostDiamond = """
        .....
        ..R..
        .RBR.
        .....
        .....
    """

    @Test
    fun `every strength step takes a capture that is on offer`() {
        val state = GameState.fromAscii(almostDiamond)

        for (config in AiConfig.STEPS) {
            assertEquals(Pos(2, 3), SearchAi(config, Random(1)).chooseMove(state), "depth ${config.depth}")
        }
    }

    @Test
    fun `looking two plies ahead saves a dot that is about to be captured`() {
        val state = GameState.fromAscii(almostDiamond, first = Player.BLUE)

        for (config in AiConfig.STEPS.filter { it.depth >= 2 }) {
            val move = SearchAi(config, Random(1)).chooseMove(state)!!
            val after = state.play(move).state
            val losingReplies = after.legalMoves().filter { after.play(it).captures.isNotEmpty() }
            assertTrue(losingReplies.isEmpty(), "depth ${config.depth} played $move and can still lose a dot")
        }
    }

    /**
     * Blue to move. The red dot at the top left and the blue dot at the bottom right are each two
     * dots from capture: blue can close in on the red dot or shore up its own.
     */
    private val attackOrDefend = """
        .........
        .........
        ..B......
        .BR......
        .........
        .........
        ......R..
        .....RB..
        .........
    """

    @Test
    fun `the threat weights decide between attacking and defending`() {
        val state = GameState.fromAscii(attackOrDefend, first = Player.BLUE)
        val attackCells = setOf(Pos(3, 3), Pos(2, 4))
        val defendCells = setOf(Pos(7, 7), Pos(6, 8))
        val base = AiConfig(depth = 1, breadth = 10, attackReach = 2, dangerReach = 2)
        val attacker = base.copy(attackWeights = listOf(3000, 2000, 120), dangerWeights = listOf(3000, 100, 20))
        val defender = base.copy(attackWeights = listOf(3000, 100, 20), dangerWeights = listOf(3000, 2000, 120))

        val attack = SearchAi(attacker, Random(1)).chooseMove(state)
        val defend = SearchAi(defender, Random(1)).chooseMove(state)
        assertTrue(attack in attackCells, "leaning to attack, it played $attack")
        assertTrue(defend in defendCells, "leaning to defence, it played $defend")
    }

    /** The [almostDiamond] in the top left corner of a larger board. */
    private val diamondInCorner = """
        ............
        ..R.........
        .RBR........
        ............
        ............
        ............
        ............
        ............
        ............
        ............
        ............
        ............
    """

    /** The [diamondInCorner], red to move after a red and a blue dot far away. */
    private fun diamondOutOfSight(): GameState =
        GameState.fromAscii(diamondInCorner).play(Pos(9, 9)).state.play(Pos(10, 10)).state

    @Test
    fun `a computer with a focus misses a capture far from the last dots`() {
        val state = diamondOutOfSight()
        val capture = Pos(2, 3)

        assertEquals(capture, SearchAi(AiConfig.NORMAL.copy(focus = 0), Random(1)).chooseMove(state))
        val focused = SearchAi(AiConfig.NORMAL, Random(1)).chooseMove(state)!!
        assertTrue(focused.x >= 7 && focused.y >= 7, "with focus 2 it played $focused, away from the last dots")
    }

    @Test
    fun `before its first dot a computer with a focus sees a capture far from the opponent's first dot`() {
        // As at the start of a level: the threat is already on the board and the opponent opens far away.
        val state = GameState.fromAscii(diamondInCorner, first = Player.BLUE).play(Pos(10, 10)).state

        for (config in AiConfig.STEPS.filter { it.focus > 0 }) {
            for (seed in 1..5) {
                assertEquals(Pos(2, 3), SearchAi(config, Random(seed)).chooseMove(state), "focus ${config.focus}, depth ${config.depth}, seed $seed")
            }
        }
    }

    @Test
    fun `no strength step plays random moves`() {
        assertTrue(AiConfig.STEPS.all { it.mistakeRate == 0.0 })
    }

    @Test
    fun `the first move on an empty board is near the centre`() {
        val move = SearchAi(AiConfig.EASY.copy(mistakeRate = 0.0), Random(1)).chooseMove(GameState.start(9, 9))

        assertEquals(Pos(4, 4), move)
    }

    @Test
    fun `no move is offered once the game is over`() {
        val over = GameState.start(5, 5, dotsPerPlayer = 0)

        assertNull(SearchAi().chooseMove(over))
    }

    @Test
    fun `computers play complete legal games`() {
        for (seed in 1..3) {
            for (config in AiConfig.STEPS - AiConfig.HARD) {
                val start = GameState.start(8, 8, dotsPerPlayer = 10)
                val end = Match.playOut(start, SearchAi(config, Random(seed)), SearchAi(config, Random(seed + 100)))

                assertTrue(end.isOver)
                assertEquals(20, end.moveCount, "seed $seed depth ${config.depth}")
            }
        }
    }

    @Test
    fun `a stronger tier plays a full game too`() {
        val start = GameState.start(8, 8, dotsPerPlayer = 6)
        val end = Match.playOut(start, SearchAi(AiConfig.HARD, Random(7)), SearchAi(AiConfig.NORMAL, Random(8)))

        assertTrue(end.isOver)
        assertEquals(12, end.moveCount)
    }

    @Test
    fun `the same seed replays the same game`() {
        fun game(): String {
            val start = GameState.start(8, 8, dotsPerPlayer = 8)
            return Match.playOut(start, SearchAi(AiConfig.NORMAL, Random(5)), SearchAi(AiConfig.NORMAL, Random(6))).toAscii()
        }

        assertEquals(game(), game())
    }

    @Test
    fun `hard answers a quiet position in more than one way`() {
        val state = quiet()

        val moves = (0 until 10).map { SearchAi(AiConfig.HARD, Random(it)).chooseMove(state) }.toSet()

        assertTrue(moves.size >= 2, "always played $moves")
    }

    @Test
    fun `without variety a quiet position gets one answer, whatever the seed`() {
        val state = quiet()
        val config = AiConfig.HARD.copy(variety = 0)

        val moves = (0 until 10).map { SearchAi(config, Random(it)).chooseMove(state) }.toSet()

        assertEquals(1, moves.size, "played $moves")
    }

    @Test
    fun `the same seed gives the same answer`() {
        val state = quiet()

        assertEquals(SearchAi(AiConfig.HARD, Random(3)).chooseMove(state), SearchAi(AiConfig.HARD, Random(3)).chooseMove(state))
    }

    /** A mid-game position with no capture or threat in reach, so many moves score about the same. */
    private fun quiet(): GameState = GameState.fromAscii(
        """
        ..........
        ..........
        ....RRB...
        .....RRB..
        ....RBBB..
        ...BBRB...
        ...BRR....
        ....B.....
        ...RRR....
        ..........
        """,
        first = Player.BLUE,
    )

    private fun wonPosition(dotsPerPlayer: Int? = null): GameState {
        val map = """
            ..R...
            .RBR..
            ......
            ......
            ......
            ......
        """
        return GameState.fromAscii(map, dotsPerPlayer).play(Pos(2, 2)).state.play(Pos(5, 5)).state
    }

    @Test
    fun `declares its dots grounded once the game is decided`() {
        val state = wonPosition()

        assertEquals(1, state.score(Player.RED))
        assertEquals(Action.Stop, SearchAi(AiConfig.HARD, Random(1)).choose(state))
    }

    @Test
    fun `keeps playing while a win is not certain and when a dot budget ends the game anyway`() {
        val fresh = GameState.start(6, 6)
        assertTrue(SearchAi(AiConfig.HARD, Random(1)).choose(fresh) is Action.Place)

        val budgeted = wonPosition(dotsPerPlayer = 5)
        assertTrue(SearchAi(AiConfig.HARD, Random(1)).choose(budgeted) is Action.Place)
    }

    @Test
    fun `collecting after a stop captures every exposed group and leaves grounded dots alone`() {
        val stopped = GameState.fromAscii(
            """
            B........
            .........
            .........
            .........
            ....B....
            .........
            .........
            .........
            .........
            """,
            first = Player.BLUE,
        ).stop()
        val ai = SearchAi(AiConfig.EASY, Random(1))

        var state = stopped
        var steps = 0
        while (!state.isOver) {
            state = state.apply(ai.choose(state)).state
            check(++steps < 100) { "collecting never finished" }
        }

        assertEquals(1, state.score(Player.RED))
        assertEquals(Player.RED, state.winner)
        assertFalse(state.isCaptured(0, 0), "a dot on the border cannot be captured")
        assertTrue(state.isCaptured(4, 4))
    }

    @Test
    fun `computers finish an open-ended game from a cross`() {
        val start = GameState.start(7, 7, dots = Openings.cross(7, 7))
        val end = Match.playOut(start, SearchAi(AiConfig.EASY, Random(1)), SearchAi(AiConfig.EASY, Random(2)))

        assertTrue(end.isOver)
    }

    @Test
    fun `collecting walls in each exposed dot with the fewest dots`() {
        // From the cross, red stops at once. Blue's own cross dots already guard two sides of each
        // red dot, so two more dots per red dot suffice: four moves for two captures.
        val stopped = GameState.start(8, 8, dots = Openings.cross(8, 8)).stop()
        val ai = SearchAi(AiConfig.EASY, Random(1))

        var state = stopped
        var steps = 0
        while (!state.isOver) {
            state = state.apply(ai.choose(state)).state
            check(++steps < 100) { "collecting never finished" }
        }

        assertEquals(2, state.score(Player.BLUE))
        assertEquals(4, state.moveCount)
        assertEquals(Player.BLUE, state.winner)
    }
}
