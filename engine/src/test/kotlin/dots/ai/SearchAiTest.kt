package dots.ai

import dots.engine.GameState
import dots.engine.Player
import dots.engine.Pos
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
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

    private val steady = listOf(
        AiConfig.EASY.copy(mistakeRate = 0.0),
        AiConfig.NORMAL.copy(mistakeRate = 0.0),
        AiConfig.HARD,
    )

    @Test
    fun `every tier takes a capture that is on offer`() {
        val state = GameState.fromAscii(almostDiamond)

        for (config in steady) {
            assertEquals(Pos(2, 3), SearchAi(config, Random(1)).chooseMove(state), "depth ${config.depth}")
        }
    }

    @Test
    fun `looking two plies ahead saves a dot that is about to be captured`() {
        val state = GameState.fromAscii(almostDiamond, first = Player.BLUE)

        for (config in steady.filter { it.depth >= 2 }) {
            val move = SearchAi(config, Random(1)).chooseMove(state)!!
            val after = state.play(move).state
            val losingReplies = after.legalMoves().filter { after.play(it).captures.isNotEmpty() }
            assertTrue(losingReplies.isEmpty(), "depth ${config.depth} played $move and can still lose a dot")
        }
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
            for (config in listOf(AiConfig.EASY, AiConfig.NORMAL)) {
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
}
