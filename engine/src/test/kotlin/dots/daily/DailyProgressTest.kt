package dots.daily

import dots.campaign.Solver
import dots.engine.Action
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DailyProgressTest {
    private val day = Day(2027, 1, 5)
    private val puzzle = Daily.puzzle(day)!!
    private val solution = Solver().solve(puzzle.level).line.map { it.action }

    /** A failed attempt: the player always takes the first free point, the computer defends. */
    private val failure: List<Action> = run {
        var state = puzzle.newGame()
        val actions = ArrayList<Action>()
        val computer = puzzle.computer(kotlin.random.Random(1))
        while (!puzzle.isOver(state)) {
            val action = if (state.toMove == puzzle.human) state.legalMoves().first().let { Action.Place(it) } else computer.choose(state)
            actions += action
            state = state.apply(action).state
        }
        check(!puzzle.isPassed(state)) { "expected a failure" }
        actions
    }

    @Test
    fun `a pass solves the day, a failure does not`() {
        val failed = DailyProgress.NONE.finished(DailyAttempt(day, 1, passed = false, failure))
        assertFalse(failed.isSolved(day))
        val solved = failed.finished(DailyAttempt(day, 2, passed = true, solution))
        assertTrue(solved.isSolved(day))
        assertEquals(2, solved.attempts(day).size)
        assertEquals(2L, solved.solution(day)?.startedAt)
    }

    @Test
    fun `the same attempt reported twice counts once`() {
        val once = DailyProgress.NONE.finished(DailyAttempt(day, 1, passed = false, failure))
        assertSame(once, once.finished(DailyAttempt(day, 1, passed = false, failure)))
    }

    @Test
    fun `an attempt belongs to its day, even if it ends after midnight`() {
        // Started on the 5th, finished on the 6th: the 5th is solved, and the streak on the 6th counts it.
        val progress = DailyProgress.NONE.finished(DailyAttempt(day, 1, passed = true, solution))
        assertTrue(progress.isSolved(day))
        assertFalse(progress.isSolved(day + 1))
        assertEquals(1, progress.streak(day + 1).days)
    }

    @Test
    fun `writes two lines of text that read back`() {
        val progress = DailyProgress.NONE
            .finished(DailyAttempt(day, 1, passed = false, failure))
            .finished(DailyAttempt(day, 2, passed = true, solution))
            .finished(DailyAttempt(day + -1, 0, passed = true, emptyList()))
        val read = DailyProgress.parse(progress.solvedText, progress.attemptsText)
        assertEquals(progress, read)
        assertEquals("2027-01-04 2027-01-05", progress.solvedText)
    }

    @Test
    fun `what cannot be read is left out`() {
        val read = DailyProgress.parse("2027-01-04 nonsense 2027-02-30 2027-01-05", "2027-01-05 1 pass e5\nbroken\n2027-01-05 x pass\n2027-01-05 2 maybe e5\n2027-01-05 3 fail ??")
        assertEquals(setOf(Day(2027, 1, 4), Day(2027, 1, 5)), read.solved)
        assertEquals(1, read.attempts.size)
        assertEquals(DailyProgress.NONE, DailyProgress.parse("", ""))
    }

    @Test
    fun `keeps the attempts of the last days only`() {
        var progress = DailyProgress.NONE
        for (n in 0 until 10) progress = progress.finished(DailyAttempt(day + n, n.toLong(), passed = true, emptyList()))
        assertEquals((3 until 10).map { day + it }, progress.attempts.map { it.day })
        assertEquals(10, progress.solved.size)
    }

    @Test
    fun `the share text ends at the pass`() {
        val progress = DailyProgress.NONE
            .finished(DailyAttempt(day, 1, passed = false, failure))
            .finished(DailyAttempt(day, 2, passed = true, solution))
        val text = progress.shareText(puzzle)
        val lines = text.lines()
        assertEquals("Pinfold #${puzzle.number} ${puzzle.best}/${puzzle.best}", lines[0])
        assertEquals(3, lines.size, text)
        assertTrue(lines[1].endsWith(DailyShare.FAILED))
        assertTrue(lines[2].endsWith(DailyShare.PASSED))
        assertNull(Regex("[a-z][0-9]").find(text))
    }
}
