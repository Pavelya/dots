package dots.campaign

import dots.ai.SearchAi
import dots.campaign.Tutorial.Lesson
import dots.campaign.Tutorial.Prompt
import dots.engine.Action
import dots.engine.Game
import dots.engine.GameState
import dots.engine.Notation
import dots.engine.Pos
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TutorialTest {
    private fun at(name: String): Pos = Notation.parse(name)!!

    private fun level(code: String) = Campaign.levels.first { it.code == code }

    private fun play(level: Level, vararg moves: String): GameState {
        val game = Game.from(LevelRecord(level, emptyList()))
        for (move in moves) game.apply(Action.Place(at(move)))
        return game.state
    }

    @Test
    fun `the tutorial is World 1's levels 1 to 3, one lesson each`() {
        assertEquals(listOf("1-1", "1-2", "1-3"), Tutorial.levels.map { it.code })
        assertEquals(listOf(Lesson.PLACING, Lesson.LINKING, Lesson.CAPTURING), Tutorial.levels.map(Tutorial::lesson))
        assertNull(Tutorial.lesson(level("1-4")))
        assertEquals(level("1-2"), Tutorial.next(level("1-1")))
        assertEquals(level("1-3"), Tutorial.next(level("1-2")))
        assertNull(Tutorial.next(level("1-3")))
        assertNull(Tutorial.next(level("1-4")))
    }

    @Test
    fun `level 1 points at the dot that closes the ring, then asks to confirm the shown dot`() {
        val first = level("1-1")
        val start = first.newGame()
        assertEquals(Tutorial.Step(Lesson.PLACING, Prompt.PLACE, at("d5")), Tutorial.step(first, start))
        assertEquals(1, Tutorial.step(first, start)!!.number)

        // A dot shown anywhere, not only on the circled point, is confirmed where it is.
        assertEquals(Tutorial.Step(Lesson.PLACING, Prompt.CONFIRM, at("a1")), Tutorial.step(first, start, preview = at("a1")))
    }

    @Test
    fun `level 2 points at the dot that joins both rings, then at the ring left open`() {
        val second = level("1-2")
        assertEquals(Tutorial.Step(Lesson.LINKING, Prompt.JOIN, at("e4")), Tutorial.step(second, second.newGame()))

        // Blue blocks d5; f5 still closes the other ring.
        val blocked = play(second, "e4", "d5")
        assertEquals(Tutorial.Step(Lesson.LINKING, Prompt.CLOSE, at("f5")), Tutorial.step(second, blocked))
    }

    @Test
    fun `level 3 starts a ring next to a blue dot of the cross`() {
        val third = level("1-3")
        val step = assertNotNull(Tutorial.step(third, third.newGame()))
        assertEquals(Prompt.SURROUND, step.prompt)
        assertEquals(3, step.number)
        val blueDots = listOf(at("e4"), at("d5"))
        val point = assertNotNull(step.point)
        assertTrue(blueDots.any { maxOf(kotlin.math.abs(it.x - point.x), kotlin.math.abs(it.y - point.y)) == 1 }, "$point is next to a blue dot")
    }

    @Test
    fun `when the computer can close a ring around the player's dots, the step blocks it`() {
        val third = level("1-3")
        // Red plays far away; Blue's c4 leaves Red's d4 one dot, d3, from capture.
        val state = play(third, "a8", "c4")
        assertEquals(Tutorial.Step(Lesson.CAPTURING, Prompt.BLOCK, at("d3")), Tutorial.step(third, state))
    }

    @Test
    fun `no step on the computer's turn, after the end, or outside the tutorial`() {
        val first = level("1-1")
        assertNull(Tutorial.step(first, play(first, "a1")), "the computer's turn")
        assertNull(Tutorial.step(first, play(first, "d5")), "the level is passed")
        val fourth = level("1-4")
        assertNull(Tutorial.step(fourth, fourth.newGame()))
    }

    @Test
    fun `following the steps passes each tutorial level against its own computer`() {
        for (level in Tutorial.levels) for (seed in 0L until 20L) {
            val game = Game.from(LevelRecord(level, emptyList()))
            val computer = SearchAi(level.ai, Random(seed))
            while (!game.isOver) {
                val state = game.state
                if (state.toMove == level.human) {
                    val step = assertNotNull(Tutorial.step(level, state), "${level.code} seed $seed: a step on the player's turn")
                    val point = assertNotNull(step.point, "${level.code} seed $seed: a point to tap in $step")
                    assertTrue(state.isLegal(point), "${level.code} seed $seed: $point is playable")
                    game.apply(Action.Place(point))
                } else {
                    game.apply(computer.choose(state))
                }
            }
            assertTrue(level.isPassed(game.state), "${level.code} seed $seed: following the steps passes")
        }
    }
}
