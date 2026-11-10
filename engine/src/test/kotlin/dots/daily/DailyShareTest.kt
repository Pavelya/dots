package dots.daily

import dots.campaign.LevelMap
import dots.campaign.Mission
import dots.campaign.Solver
import dots.engine.Action
import dots.engine.Notation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class DailyShareTest {

    // Red can close round c3 at d3; blue can close round e5 at e6.
    private val puzzle = DailyPuzzle(
        Day(2027, 1, 10),
        LevelMap(
            listOf(
                "......",
                "..R...",
                ".RB...",
                "..R.B.",
                "...BRB",
                "......",
            ),
        ),
        Mission.Capture(1, 2),
    )

    private fun moves(vararg words: String): List<Action> = words.map { Action.Place(Notation.parse(it)!!) }

    @Test
    fun `one row per attempt, one symbol per move`() {
        val lost = moves("a1", "e6", "a6")
        val passed = moves("d3")
        assertEquals("Pinfold #10 1/2\n🔵⚪ ❌\n🔴 ✅", DailyShare.text(puzzle, listOf(lost, passed)))
    }

    @Test
    fun `no pass, and an attempt still going`() {
        val quiet = moves("a1", "f1", "a6")
        assertEquals("Pinfold #10 X/2\n⚪⚪ ❌\n⚪", DailyShare.text(puzzle, listOf(quiet, moves("a1", "f1"))))
    }

    @Test
    fun `says nothing of the board or the moves`() {
        val text = DailyShare.text(puzzle, listOf(moves("d3")))
        assertFalse(Regex("[a-f][1-6]").containsMatchIn(text), text)
        assertFalse(text.contains('R') || text.contains('B'), text)
    }

    @Test
    fun `a shipped puzzle played the solver's way`() {
        val shipped = Daily.year(2027).first { it.mission is Mission.Capture && it.best == 3 }
        val line = Solver().solve(shipped.level).line.map { it.action }
        val text = DailyShare.text(shipped, listOf(line))
        assertEquals("Pinfold #${shipped.number} 3/3", text.lines().first())
        assert(text.lines()[1].endsWith("🔴 ✅")) { text }
    }

    @Test
    fun `refuses moves past the end or against the rules`() {
        assertFailsWith<IllegalArgumentException> { DailyShare.text(puzzle, listOf(moves("d3", "a1"))) }
        assertFailsWith<IllegalArgumentException> { DailyShare.text(puzzle, listOf(moves("c3"))) }
    }
}
