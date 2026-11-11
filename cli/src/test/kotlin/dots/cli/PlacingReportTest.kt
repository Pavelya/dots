package dots.cli

import dots.engine.GameRecord
import dots.engine.GameSettings
import dots.log.GameLogEntry
import dots.log.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlacingReportTest {
    private var started = 0L

    private fun game(placing: String, touches: Int, placed: Int, undos: Int = 0, opponent: String = "normal") = GameLogEntry(
        appVersion = "0.2",
        startedAt = started++,
        updatedAt = 0,
        playMillis = 0,
        opponent = opponent,
        threatHighlight = false,
        undos = undos,
        outcome = Outcome.FINISHED,
        red = 0,
        blue = 0,
        record = GameRecord(GameSettings.POC, emptyList()),
        placing = placing,
        touches = touches,
        placed = placed,
    )

    @Test
    fun `each method gets its undos per game against the computer and its taps per dot`() {
        val rows = PlacingReport.numbers(
            listOf(
                game("two-taps", touches = 70, placed = 33, undos = 1),
                game("two-taps", touches = 66, placed = 33),
                game("magnifier", touches = 36, placed = 33, undos = 2),
                // Two people on one phone: their taps count, but they have no undo.
                game("magnifier", touches = 30, placed = 30, opponent = GameLogEntry.HUMAN),
                game("", touches = 0, placed = 0, undos = 5), // logged before the method was recorded
            ),
        )
        assertEquals(listOf("two-taps", "magnifier"), rows.map { it.method })
        val (twoTaps, magnifier) = rows
        assertEquals(0.5, twoTaps.undosPerGame)
        assertEquals(136.0 / 66, twoTaps.tapsPerDot)
        assertEquals(2, magnifier.games)
        assertEquals(2.0, magnifier.undosPerGame)
        assertEquals(66.0 / 63, magnifier.tapsPerDot)
    }

    @Test
    fun `a method with no computer games or no dots has no number rather than a wrong one`() {
        val row = PlacingReport.numbers(listOf(game("zoom", touches = 0, placed = 0, opponent = GameLogEntry.HUMAN))).single()
        assertNull(row.undosPerGame)
        assertNull(row.tapsPerDot)
    }

    @Test
    fun `the report lists every tester, then everyone`() {
        val ann = PocReport.Tester("ann", listOf(game("drag", touches = 34, placed = 33, undos = 1)))
        val bob = PocReport.Tester("bob", listOf(game("two-taps", touches = 67, placed = 33)))
        val text = PlacingReport.format(listOf(ann, bob))
        assertTrue("ann:" in text && "bob:" in text && "Everyone:" in text, text)
        assertTrue(Regex("""drag\s+1\s+1\.00\s+1\.03""").containsMatchIn(text), text)
        assertTrue(Regex("""two-taps\s+1\s+0\.00\s+2\.03""").containsMatchIn(text), text)
    }
}
