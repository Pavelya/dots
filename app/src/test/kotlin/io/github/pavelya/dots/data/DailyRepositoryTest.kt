package io.github.pavelya.dots.data

import dots.campaign.Campaign
import dots.campaign.Progress
import dots.campaign.Solver
import dots.daily.Daily
import dots.daily.DailyClock
import dots.daily.DailyPuzzle
import dots.daily.Day
import dots.engine.Action
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.TimeZone

/**
 * The daily puzzle's streak with a clock the test sets (A-07): across midnight, a change of time
 * zone, a missed day, and a restart of the app.
 */
class DailyRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val hour = 3_600_000L
    private val minute = 60_000L

    /** The test's clock: the time now and the phone's time zone. */
    private var now = 0L
    private var zone: TimeZone = TimeZone.getTimeZone("Europe/Berlin")
    private val clock = PuzzleClock(millis = { now }, zone = { zone })

    /** Opens the progress file as the app would after a start; close() is what a restart does. */
    private inner class Session(file: File) {
        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.IO + job)
        private val store = ProgressRepository.store(scope) { file }
        val daily = DailyRepository(store, scope) { clock }
        val progress = ProgressRepository(store, scope)

        suspend fun close() = job.cancelAndJoin()
    }

    private fun file() = folder.root.resolve("progress.preferences_pb")

    /** [hours] and [minutes] past midnight on [day], in the test's time zone. */
    private fun at(day: Day, hours: Int, minutes: Int = 0): Long {
        val utc = day.epochDay * DailyClock.DAY_MILLIS + hours * hour + minutes * minute
        return utc - zone.getOffset(utc)
    }

    private fun puzzle(day: Day): DailyPuzzle = Daily.puzzle(day)!!

    private fun solution(puzzle: DailyPuzzle): List<Action> = Solver().solve(puzzle.level).line.map { it.action }

    private suspend fun DailyRepository.solve(day: Day, startedAt: Long = now): DailyEnd {
        val puzzle = puzzle(day)
        return finished(puzzle, startedAt, solution(puzzle), passed = true)
    }

    // Monday 4 January 2027 starts a week.
    private val monday = Day(2027, 1, 4)

    @Test
    fun `today follows the clock across midnight`() {
        now = at(monday, 23, 59)
        assertEquals(monday, clock.today())
        assertEquals(minute, clock.toNextPuzzle())
        now += minute
        assertEquals(monday + 1, clock.today())
        assertEquals(24 * hour, clock.toNextPuzzle())
    }

    @Test
    fun `a change of time zone changes the day at once`() {
        now = at(monday, 23, 30) // Berlin
        assertEquals(monday, clock.today())
        zone = TimeZone.getTimeZone("Asia/Tokyo")
        assertEquals(monday + 1, clock.today())
        zone = TimeZone.getTimeZone("America/New_York")
        assertEquals(monday, clock.today())
    }

    @Test
    fun `an attempt that ends after midnight counts for the day it began`() = runBlocking {
        val session = Session(file())
        val started = at(monday, 23, 55)
        now = at(monday + 1, 0, 5)
        val end = session.daily.solve(monday, startedAt = started)
        val progress = session.daily.progress.first()
        assertTrue(progress.isSolved(monday))
        assertFalse(progress.isSolved(monday + 1))
        assertEquals(1, end.streak.days)
        // Tuesday's puzzle is the next one: not solved yet, the streak still stands.
        assertEquals(monday + 1, clock.today())
        assertEquals(puzzle(monday + 1), session.daily.today())
        session.close()
    }

    @Test
    fun `a missed day uses the week's freeze, a second one ends the streak`() = runBlocking {
        val session = Session(file())
        for (n in 0..2) {
            now = at(monday + n, 9)
            session.daily.solve(monday + n)
        }
        // Thursday missed: on Friday the freeze keeps the streak, and none is left this week.
        now = at(monday + 4, 9)
        var streak = session.daily.progress.first().streak(clock.today())
        assertEquals(3, streak.days)
        assertFalse(streak.freezeLeft)
        assertEquals(4, session.daily.solve(monday + 4).streak.days)
        // Saturday missed too: on Sunday the streak is over.
        now = at(monday + 6, 9)
        streak = session.daily.progress.first().streak(clock.today())
        assertEquals(0, streak.days)
        // Monday brings a new week's freeze.
        now = at(monday + 7, 9)
        assertTrue(session.daily.progress.first().streak(clock.today()).freezeLeft)
        session.close()
    }

    @Test
    fun `the streak survives a restart and is kept apart from the campaign`() = runBlocking {
        val first = Session(file())
        now = at(monday, 12)
        first.daily.solve(monday)
        first.progress.record(Campaign.level(1, 1), 3)
        // The developer menu's sample progress replaces the stars only.
        first.progress.replace(Progress.NONE)
        first.close()

        val second = Session(file())
        now = at(monday + 1, 12)
        assertEquals(1, second.daily.progress.first().streak(clock.today()).days)
        assertEquals(Progress.NONE, second.progress.progress.first())
        second.daily.clear()
        assertEquals(0, second.daily.progress.first().streak(clock.today()).days)
        second.close()
    }

    @Test
    fun `a failed attempt saves no streak and no share text`() = runBlocking {
        val session = Session(file())
        now = at(monday, 12)
        val end = session.daily.finished(puzzle(monday), now, emptyList(), passed = false)
        assertFalse(end.passed)
        assertNull(end.share)
        assertEquals(0, end.streak.days)
        assertFalse(session.daily.progress.first().isSolved(monday))
        session.close()
    }

    @Test
    fun `before the first puzzle there is none, and a year ahead finds one`() {
        now = at(Day(2026, 11, 15), 12)
        val session = Session(file())
        assertNull(session.daily.today())
        clock.shiftDays = PuzzleClock.YEAR_AHEAD_DAYS
        assertEquals(Day(2027, 11, 15), clock.today())
        assertEquals(Day(2027, 11, 15), session.daily.today()?.day)
        runBlocking { session.close() }
    }
}
