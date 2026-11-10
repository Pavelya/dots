package dots.daily

import dots.campaign.LevelMap
import dots.campaign.Mission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DailyGeneratorTest {

    @Test
    fun `random numbers are SplitMix64's`() {
        // The first numbers of SplitMix64 from seed 0, as its reference code gives them.
        val random = SplitMix64(0)
        assertEquals(0xE220A8397B1DCDAFuL.toLong(), random.nextLong())
        assertEquals(0x6E789E6AA1B965F4uL.toLong(), random.nextLong())
        assertEquals(0x06C45D188009454FuL.toLong(), random.nextLong())
    }

    /**
     * The shipped puzzles were made on another machine, so making them again here checks that a
     * day gives the same puzzle everywhere. A failure means the generator, the solver or the rules
     * now choose differently. The shipped puzzles must not change, since players share and
     * compare them: undo the change, or leave the shipped years as they are and only make new
     * years with it, then change this test to those years.
     */
    @Test
    fun `the same day gives the same puzzle as the shipped one`() {
        val shipped = Daily.year(2027)
        val days = listOf(0, 1, 2, 45, 90, 135, 180, 225, 270, 315, 364)
        for (i in days) {
            val puzzle = shipped[i]
            assertEquals(puzzle, DailyGenerator.generate(puzzle.day), "the puzzle of ${puzzle.day}")
        }
    }

    @Test
    fun `different days give different puzzles`() {
        val day = Day(2031, 6, 1)
        val first = DailyGenerator.generate(day)
        val second = DailyGenerator.generate(day + 1)
        assertEquals(first, DailyGenerator.generate(day))
        assert(first.map != second.map)
    }

    @Test
    fun `a generated puzzle is fair`() {
        val puzzle = DailyGenerator.generate(Day(2030, 2, 14))
        assertNull(DailyGenerator.problem(puzzle))
    }

    @Test
    fun `finds what makes a puzzle unfair`() {
        val day = Day(2027, 1, 1)
        // Red closes the ring round c3 at d3 at once.
        val oneMove = LevelMap(
            listOf(
                ".....",
                "..R..",
                ".RB..",
                "..R..",
                ".....",
            ),
        )
        assertEquals("it passes in 1 move, not 2", DailyGenerator.problem(DailyPuzzle(day, oneMove, Mission.Capture(1, 2))))
        // Any first move is safe: blue cannot capture anything in two moves.
        val nothing = LevelMap(
            listOf(
                "......",
                "......",
                "..RB..",
                "..BR..",
                "......",
                "......",
            ),
        )
        assertEquals("more than one first move passes it", DailyGenerator.problem(DailyPuzzle(day, nothing, Mission.Survive(2))))
        assertEquals("the solver's verdict is unsolvable", DailyGenerator.problem(DailyPuzzle(day, nothing, Mission.Capture(1, 2))))
        assertEquals("it takes 5 moves, not 2 to 4", DailyGenerator.problem(DailyPuzzle(day, nothing, Mission.Survive(5))))
    }
}
