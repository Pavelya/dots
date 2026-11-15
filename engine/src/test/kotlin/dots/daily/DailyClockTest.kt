package dots.daily

import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class DailyClockTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val berlin = TimeZone.getTimeZone("Europe/Berlin")
    private val newYork = TimeZone.getTimeZone("America/New_York")
    private val tokyo = TimeZone.getTimeZone("Asia/Tokyo")

    private val hour = 3_600_000L
    private val minute = 60_000L

    /** Midnight UTC at the start of [day]. */
    private fun midnightUtc(day: Day) = day.epochDay * DailyClock.DAY_MILLIS

    @Test
    fun `a new day at midnight`() {
        val midnight = midnightUtc(Day(2027, 1, 5))
        assertEquals(Day(2027, 1, 4), DailyClock.today(midnight - 1, utc))
        assertEquals(Day(2027, 1, 5), DailyClock.today(midnight, utc))
    }

    @Test
    fun `the day is the phone's own, in its time zone`() {
        // 23:30 UTC on 4 January is already the 5th in Berlin and Tokyo, still the 4th in New York.
        val now = midnightUtc(Day(2027, 1, 5)) - 30 * minute
        assertEquals(Day(2027, 1, 4), DailyClock.today(now, utc))
        assertEquals(Day(2027, 1, 5), DailyClock.today(now, berlin))
        assertEquals(Day(2027, 1, 5), DailyClock.today(now, tokyo))
        assertEquals(Day(2027, 1, 4), DailyClock.today(now, newYork))
    }

    @Test
    fun `the time to the next puzzle`() {
        val midnight = midnightUtc(Day(2027, 1, 5))
        assertEquals(minute, DailyClock.toNextPuzzle(midnight - minute, utc))
        assertEquals(24 * hour, DailyClock.toNextPuzzle(midnight, utc))
        // Berlin is an hour ahead in winter: its midnight is 23:00 UTC.
        assertEquals(30 * minute, DailyClock.toNextPuzzle(midnight - 90 * minute, berlin))
    }

    @Test
    fun `a day the clocks change on is shorter or longer`() {
        // New York moves to summer time on Sunday 14 March 2027 and back on Sunday 7 November.
        val springStart = midnightUtc(Day(2027, 3, 14)) + 5 * hour // midnight in New York (UTC-5)
        assertEquals(Day(2027, 3, 14), DailyClock.today(springStart, newYork))
        assertEquals(23 * hour, DailyClock.toNextPuzzle(springStart, newYork))
        val autumnStart = midnightUtc(Day(2027, 11, 7)) + 4 * hour // midnight in New York (UTC-4)
        assertEquals(Day(2027, 11, 7), DailyClock.today(autumnStart, newYork))
        assertEquals(25 * hour, DailyClock.toNextPuzzle(autumnStart, newYork))
    }

    @Test
    fun `the next puzzle is always ahead`() {
        for (zone in listOf(utc, berlin, newYork, tokyo)) {
            var now = midnightUtc(Day(2027, 3, 13))
            repeat(24 * 8) {
                val left = DailyClock.toNextPuzzle(now, zone)
                assert(left in 1..25 * hour) { "${zone.id} at $now: $left" }
                assertEquals(DailyClock.today(now, zone) + 1, DailyClock.today(now + left, zone), "${zone.id} at $now")
                now += 31 * minute
            }
        }
    }
}
