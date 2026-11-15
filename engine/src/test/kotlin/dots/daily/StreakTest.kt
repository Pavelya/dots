package dots.daily

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreakTest {
    // Monday 4 January 2027 to Sunday 10 January is one week; Monday 11 January starts the next.
    private val monday = Day(2027, 1, 4)

    private fun day(n: Int) = monday + n

    private fun streak(today: Int, vararg solved: Int) = Streak.of(solved.map(::day), day(today))

    @Test
    fun `weeks run Monday to Sunday`() {
        assertEquals(Streak.week(day(0)), Streak.week(day(6)))
        assertEquals(Streak.week(day(0)) + 1, Streak.week(day(7)))
        assertEquals(Streak.week(day(-1)) + 1, Streak.week(day(0)))
    }

    @Test
    fun `nothing solved, no streak`() {
        val streak = Streak.of(emptyList(), day(3))
        assertEquals(0, streak.days)
        assertTrue(streak.freezeLeft)
    }

    @Test
    fun `days in a row`() {
        assertEquals(3, streak(2, 0, 1, 2).days)
    }

    @Test
    fun `today does not break the streak while it lasts`() {
        val streak = streak(3, 0, 1, 2)
        assertEquals(3, streak.days)
        assertTrue(streak.freezeLeft)
        assertEquals(emptyList(), streak.frozen)
    }

    @Test
    fun `a missed day uses the week's freeze`() {
        val streak = streak(3, 0, 2, 3)
        assertEquals(3, streak.days)
        assertEquals(listOf(day(1)), streak.frozen)
        assertFalse(streak.freezeLeft)
    }

    @Test
    fun `yesterday missed, today not solved yet, the freeze keeps the streak`() {
        val streak = streak(4, 0, 1, 2)
        assertEquals(3, streak.days)
        assertEquals(listOf(day(3)), streak.frozen)
        assertFalse(streak.freezeLeft)
    }

    @Test
    fun `a second missed day in one week ends the streak`() {
        assertEquals(0, streak(5, 0, 1, 2).days)
        // Solving again starts a new streak, with no freeze left this week.
        val again = streak(5, 0, 2, 4, 5)
        assertEquals(2, again.days)
        assertFalse(again.freezeLeft)
    }

    @Test
    fun `each week has its own freeze, and a new week brings one back`() {
        // Missed Wednesday 6 January and Tuesday 12 January.
        val streak = streak(9, 0, 1, 3, 4, 5, 6, 7, 9)
        assertEquals(8, streak.days)
        assertEquals(listOf(day(2), day(8)), streak.frozen)
        assertFalse(streak.freezeLeft)
        assertTrue(streak(7, 0, 1, 3, 4, 5, 6, 7).freezeLeft)
    }

    @Test
    fun `Sunday and Monday missed are frozen by two weeks`() {
        val streak = streak(8, 0, 1, 2, 3, 4, 5)
        assertEquals(6, streak.days)
        assertEquals(listOf(day(6), day(7)), streak.frozen)
    }

    @Test
    fun `freezes do not save up`() {
        // Two whole weeks solved, then two days missed in the third.
        val solved = (0..13).toList() + 16
        assertEquals(1, Streak.of(solved.map(::day), day(16)).days)
    }

    @Test
    fun `the streak ends when days pass unplayed`() {
        assertEquals(0, streak(10, 0, 1, 2).days)
    }

    @Test
    fun `a day solved after today still counts`() {
        // The clock was set back a day after solving day 3, or the player flew west.
        val streak = streak(2, 0, 1, 3)
        assertEquals(3, streak.days)
        assertEquals(emptyList(), streak.frozen)
        assertTrue(streak.freezeLeft)
    }

    @Test
    fun `the order the days come in does not matter`() {
        assertEquals(streak(3, 0, 1, 2, 3).days, Streak.of(listOf(day(3), day(0), day(2), day(1)), day(3)).days)
    }
}
