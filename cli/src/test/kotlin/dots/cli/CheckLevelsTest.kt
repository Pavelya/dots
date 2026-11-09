package dots.cli

import dots.campaign.Campaign
import dots.campaign.Level
import dots.campaign.LevelMap
import dots.campaign.Mission
import dots.campaign.Solutions
import dots.campaign.Strength
import dots.cli.CheckLevels.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheckLevelsTest {

    /** This is what keeps a bad puzzle out of the campaign: CI runs it on every pull request. */
    @Test
    fun `every campaign level passes check-levels`() {
        val failed = Campaign.levels.map { CheckLevels.check(it) }.filter { it.result == Result.FAILED }

        assertTrue(failed.isEmpty(), failed.joinToString("\n") { "${it.level.code}: ${it.note}" })
    }

    private fun level(mission: Mission, map: List<String>, solutions: Solutions = Solutions.FEW) =
        Level(1, 1, "Test", Strength.EASY, 6, LevelMap(map), mission, solutions = solutions)

    @Test
    fun `a puzzle with one answer passes and shows it`() {
        val check = CheckLevels.check(level(Mission.Capture(1, 1), listOf(".....", "..R..", ".RB..", "..R..", ".....")))

        assertEquals(Result.PASSED, check.result, check.note)
        assertEquals("passes in 1 move: d3; 1 first move passes: d3", check.note)
    }

    @Test
    fun `a puzzle with many answers fails unless its file says open`() {
        // Blue cannot capture anything with one more dot, so every first move survives.
        val map = listOf(".....", ".....", "..R..", ".....", "....B")

        val few = CheckLevels.check(level(Mission.Survive(1), map))
        assertEquals(Result.FAILED, few.result)
        assertTrue(few.note.startsWith("too easy: passes in 1 move: "), few.note)
        assertTrue("at least 4 first moves pass" in few.note, few.note)

        assertEquals(Result.PASSED, CheckLevels.check(level(Mission.Survive(1), map, Solutions.OPEN)).result)
    }

    @Test
    fun `an unsolvable puzzle fails`() {
        val check = CheckLevels.check(level(Mission.Capture(1, 2), listOf(".....", "..R..", ".RB..", ".....", ".....")))

        assertEquals(Result.FAILED, check.result)
        assertEquals("unsolvable: the computer can stop every try within 2 moves", check.note)
    }

    @Test
    fun `whole games and long missions are not checked`() {
        val map = listOf(".....", "..R..", ".RB..", ".....", ".....")

        for (mission in listOf(Mission.Win, Mission.WinBy(2), Mission.Capture(1, 6))) {
            val check = CheckLevels.check(level(mission, map))
            assertEquals(Result.NOT_CHECKED, check.result, mission.text)
        }
    }

    @Test
    fun `a line shows the computer's replies in brackets`() {
        val check = CheckLevels.check(level(Mission.Capture(1, 2), listOf(".......", "..R.R..", ".RB.BR.", ".......", ".......")))

        assertEquals(Result.PASSED, check.result, check.note)
        assertTrue(Regex("passes in 2 moves: d3 \\([a-g]\\d\\) [a-g]\\d; 1 first move passes: d3").matches(check.note), check.note)
    }
}
