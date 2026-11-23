package dots.daily

import dots.campaign.Solver
import dots.engine.Player
import dots.log.GameLogEntry
import dots.log.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DailyRecordTest {
    private val puzzle = Daily.puzzle(Day(2027, 1, 5))!!
    private val solution = Solver().solve(puzzle.level).line.map { it.action }

    @Test
    fun `writes one line that reads back`() {
        val record = DailyRecord(puzzle, solution)
        val text = record.toText()
        assertTrue(text.startsWith("daily/1 2027-01-05 |"), text)
        assertEquals(record, DailyRecord.parse(text))
        assertEquals(DailyRecord(puzzle, emptyList()), DailyRecord.parse("daily/1 2027-01-05 |"))
    }

    @Test
    fun `replays to the end of the puzzle`() {
        val record = DailyRecord(puzzle, solution)
        val end = record.replay()
        assertTrue(record.isOver(end))
        assertTrue(puzzle.isPassed(end))
    }

    @Test
    fun `a day with no puzzle, or a bad line, is refused`() {
        assertFailsWith<NoSuchElementException> { DailyRecord.parse("daily/1 2026-11-15 |") }
        assertFailsWith<IllegalArgumentException> { DailyRecord.parse("daily/1 15-11-2026 |") }
        assertFailsWith<IllegalArgumentException> { DailyRecord.parse("level/2 1-1 |") }
    }

    @Test
    fun `the game log reads a daily game`() {
        val record = DailyRecord(puzzle, solution)
        val end = record.replay()
        val entry = GameLogEntry.start(record, puzzle.strength.text, threatHighlight = false, now = 1)
            .copy(record = record, outcome = Outcome.FINISHED, red = end.score(Player.RED), blue = end.score(Player.BLUE))
        val read = GameLogEntry.parse(entry.toJson())
        assertEquals(entry, read)
        assertNull(read.check())
    }
}
