package dots.daily

import dots.campaign.LevelFormatException
import dots.campaign.Mission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The shipped puzzles of 2027. */
class DailyTest {

    private val year = Daily.year(2027)

    @Test
    fun `every day of 2027 has one puzzle, in date order`() {
        assertEquals(Day.allOf(2027), year.map { it.day })
        assertEquals(Day(2027, 3, 14), Daily.puzzle(Day(2027, 3, 14))?.day)
        assertNull(Daily.puzzle(Day(2026, 12, 31)))
        assertEquals(emptyList(), Daily.year(1999))
    }

    @Test
    fun `puzzles are numbered from the first day`() {
        assertEquals(1, year.first().number)
        assertEquals(365, year.last().number)
        assertEquals(366, Daily.number(Day(2028, 1, 1)))
    }

    /** The check of G-12: every shipped puzzle passes the solver, with one first move in 2 to 4 moves. */
    @Test
    fun `every puzzle is fair`() {
        val problems = year.parallelStream()
            .map { puzzle -> DailyGenerator.problem(puzzle)?.let { "${puzzle.day}: $it" } }
            .toList().filterNotNull()
        assertEquals(emptyList(), problems)
    }

    @Test
    fun `the year has every kind of mission`() {
        val kinds = year.map { puzzle ->
            when (val mission = puzzle.mission) {
                is Mission.Capture -> "capture ${mission.dots} in ${mission.moves}"
                is Mission.Survive -> "survive ${mission.moves}"
                else -> mission.text
            }
        }.toSet()
        val expected = (2..4).flatMap { listOf("capture 1 in $it", "capture 2 in $it", "survive $it") }.toSet()
        assertEquals(expected, kinds)
    }

    @Test
    fun `the file is written as the format writes it`() {
        val text = Daily::class.java.getResourceAsStream("/daily/2027.txt")!!.use { it.readBytes().toString(Charsets.UTF_8) }
        assertEquals(DailyFormat.write(year), text)
    }

    @Test
    fun `refuses a file out of order or of another year`() {
        val a = year[1]
        val b = year[0]
        val swapped = DailyFormat.write(listOf(a, b))
        val error = assertFailsWith<LevelFormatException> { Daily.load(2027) { swapped } }
        assertTrue("comes after" in error.message!!, error.message)
        val other = assertFailsWith<LevelFormatException> { Daily.load(2028) { swapped } }
        assertTrue("is not in 2028" in other.message!!, other.message)
    }
}
