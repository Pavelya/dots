package dots.daily

import dots.campaign.LevelFormatException
import dots.campaign.LevelMap
import dots.campaign.Mission
import dots.campaign.Strength
import dots.engine.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DailyFormatTest {

    private val map = LevelMap(
        listOf(
            "......",
            "..R...",
            ".RB...",
            "..RB..",
            "...B..",
            "......",
        ),
    )

    private val text = """
        date: 2027-05-01
        computer: hard
        mission: capture 1 in 3 moves
        map:
        ......
        ..R...
        .RB...
        ..RB..
        ...B..
        ......

        date: 2027-05-02
        computer: hard
        mission: survive 2 moves
        map:
        ......
        ..R...
        .RB...
        ..RB..
        ...B..
        ......

    """.trimIndent()

    @Test
    fun `writes and reads puzzles`() {
        val puzzles = listOf(
            DailyPuzzle(Day(2027, 5, 1), map, Mission.Capture(1, 3)),
            DailyPuzzle(Day(2027, 5, 2), map, Mission.Survive(2)),
        )
        assertEquals(text, DailyFormat.write(puzzles))
        assertEquals(puzzles, DailyFormat.parse(text))
        assertEquals(3, puzzles[0].dots)
        assertEquals(Strength.HARD, puzzles[1].strength)
    }

    @Test
    fun `points at the line of the file`() {
        fun error(text: String) = assertFailsWith<LevelFormatException> { DailyFormat.parse(text, "t") }
        val badMission = error(text.replace("survive 2 moves", "survive often"))
        assertEquals(14, badMission.line)
        val badDate = error(text.replace("2027-05-02", "2027-05-32"))
        assertEquals(12, badDate.line)
        val badRow = error(text.replace("...B..\n......\n\ndate", "...B.\n......\n\ndate"))
        assertEquals(9, badRow.line)
        assertEquals(1, error("computer: hard\n$text").line)
    }

    @Test
    fun `refuses a mission a daily puzzle does not have`() {
        val error = assertFailsWith<LevelFormatException> { DailyFormat.parse(text.replace("capture 1 in 3 moves", "target c3"), "t") }
        assertEquals(1, error.line)
        assertFailsWith<IllegalArgumentException> { DailyPuzzle(Day(2027, 1, 1), map, Mission.Target(Pos(2, 2))) }
    }
}
