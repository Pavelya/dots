package dots.ai

import dots.engine.Action
import dots.engine.GameSettings
import dots.engine.GameState
import dots.engine.Pos
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

class TimeLimitTest {
    /** A 15x15 POC game, 20 moves in, so there is plenty to think about. */
    private fun midGame(): GameState {
        var state = GameSettings.POC.copy(seed = 11).newGame()
        val ai = SearchAi(AiConfig.EASY.copy(mistakeRate = 0.0), Random(1))
        repeat(20) { state = state.apply(ai.choose(state)).state }
        return state
    }

    /** Far too deep to finish: only the time limit can stop it. */
    private val slow = AiConfig(depth = 9, breadth = 12, attackReach = 3, dangerReach = 3)

    @Test
    fun `answers within the time limit with a legal move`() {
        val state = midGame()
        val started = TimeSource.Monotonic.markNow()
        val action = SearchAi(slow.copy(timeLimitMillis = 300), Random(2)).choose(state)
        val took = started.elapsedNow().inWholeMilliseconds

        assertTrue(action is Action.Place && state.isLegal(action.pos), "$action")
        assertTrue(took < 400, "took $took ms with a 300 ms limit")
    }

    @Test
    fun `a cancelled search returns at once with a legal move`() {
        val state = midGame()
        var calls = 0
        val started = TimeSource.Monotonic.markNow()
        val action = SearchAi(slow, Random(3)).choose(state) { ++calls > 50 }
        val took = started.elapsedNow().inWholeMilliseconds

        assertTrue(action is Action.Place && state.isLegal(action.pos), "$action")
        assertTrue(took < 300, "took $took ms after being cancelled")
    }

    @Test
    fun `with a time limit it still takes a capture on offer`() {
        val state = GameState.fromAscii(
            """
            .......
            ...R...
            ..RBR..
            .......
            .......
            """,
        )
        val action = SearchAi(AiConfig.HARD.copy(timeLimitMillis = 200), Random(5)).choose(state)

        assertEquals(Action.Place(Pos(3, 3)), action)
    }
}
