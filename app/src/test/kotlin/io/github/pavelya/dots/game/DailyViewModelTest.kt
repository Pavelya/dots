package io.github.pavelya.dots.game

import dots.ai.Ai
import dots.campaign.Solver
import dots.daily.Daily
import dots.daily.DailyAttempt
import dots.daily.DailyClock
import dots.daily.DailyProgress
import dots.daily.DailyPuzzle
import dots.daily.DailyRecord
import dots.daily.Day
import dots.engine.Action
import dots.engine.GameState
import dots.engine.Player
import dots.log.GameLogEntry
import dots.log.Outcome
import io.github.pavelya.dots.data.DailyEnd
import io.github.pavelya.dots.data.DailyJournal
import io.github.pavelya.dots.data.GameJournal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Keeps every entry the game hands over. */
private class Entries : GameJournal {
    val started = mutableListOf<GameLogEntry>()
    val saved = mutableListOf<GameLogEntry>()
    val finished = mutableListOf<GameLogEntry>()
    override fun start(entry: GameLogEntry) { started += entry }
    override fun save(entry: GameLogEntry) { saved += entry }
    override fun finish(entry: GameLogEntry) { finished += entry }
}

/** Keeps the daily progress in memory, as [io.github.pavelya.dots.data.DailyRepository] keeps it in the store. */
private class Days(private val today: () -> Day) : DailyJournal {
    var progress = DailyProgress.NONE
    val reported = mutableListOf<DailyAttempt>()

    override suspend fun finished(puzzle: DailyPuzzle, startedAt: Long, actions: List<Action>, passed: Boolean): DailyEnd {
        val attempt = DailyAttempt(puzzle.day, startedAt, passed, actions)
        reported += attempt
        progress = progress.finished(attempt)
        return DailyEnd.of(puzzle, attempt, progress, today())
    }
}

/** The computer's actions of [line] (every second one, from the second), one after the other. */
private class Replies(line: List<Action>) : Ai {
    private val replies = line.filterIndexed { i, _ -> i % 2 == 1 }.iterator()
    override fun choose(state: GameState): Action = replies.next()
}

/** The daily puzzle in the game (A-07): no undo, the attempt belongs to its day, Result after it is saved. */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    init {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val day = Day(2027, 1, 5)
    private val puzzle = Daily.puzzle(day)!!

    /** The solver's line: the player's moves and the computer's best replies, in turn. */
    private val line: List<Action> = Solver().solve(puzzle.level).line.map { it.action }

    /** 23:59 on the puzzle's day, in UTC. */
    private val lateEvening = (day.epochDay + 1) * DailyClock.DAY_MILLIS - 60_000

    private fun game(
        journal: GameJournal? = null,
        daily: DailyJournal? = null,
        start: GameLogEntry = dailyGame(puzzle, threatHighlight = false, now = lateEvening),
        clock: () -> Long = { lateEvening },
        ai: Ai = Replies(line),
    ) = GameViewModel(
        start = start,
        nextGame = { dailyGame(puzzle, threatHighlight = false, now = clock()) },
        computerFor = { ai },
        journal = journal,
        computerDispatcher = dispatcher,
        replyDelayMillis = 0,
        clock = clock,
        daily = daily,
    )

    /** Plays the player's moves of the solver's line; the computer answers as the line says. */
    private suspend fun GameViewModel.playLine(advance: suspend () -> Unit) {
        for (action in line.filterIndexed { i, _ -> i % 2 == 0 }) {
            val place = action as Action.Place
            onTap(place.pos)
            onTap(place.pos)
            advance()
        }
    }

    @Test
    fun `the daily puzzle has no undo and no boosters`() = runTest(dispatcher) {
        val vm = game()
        val first = line.first() as Action.Place
        vm.onTap(first.pos)
        vm.onTap(first.pos)
        advanceUntilIdle()
        val ui = vm.ui.value
        assertFalse(ui.canUndo)
        assertTrue(ui.usable.isEmpty())
        assertNotNull(ui.daily)
        assertNull(ui.level)
        assertFalse(ui.openEnded)
        val before = ui.state
        vm.undo()
        assertEquals(before, vm.ui.value.state)
    }

    @Test
    fun `an attempt that runs past midnight belongs to its own day`() = runTest(dispatcher) {
        var now = lateEvening
        val days = Days { DailyClockTestDays.of(now) }
        val vm = game(daily = days, clock = { now })
        // Midnight passes during the attempt.
        now = lateEvening + 10 * 60_000
        vm.playLine { advanceUntilIdle() }
        advanceUntilIdle()
        assertTrue(vm.ui.value.isOver)
        val attempt = days.reported.single()
        assertEquals(day, attempt.day)
        assertTrue(attempt.passed)
        assertEquals(lateEvening, attempt.startedAt)
        assertTrue(days.progress.isSolved(day))
        // On the next day the streak counts it.
        assertEquals(1, vm.ui.value.dailyEnd?.streak?.days)
    }

    @Test
    fun `Result opens once the attempt is saved, with the moves, the best and the share text`() = runTest(dispatcher) {
        val days = Days { day }
        val vm = game(daily = days)
        vm.playLine { advanceUntilIdle() }
        advanceUntilIdle()
        val ui = vm.ui.value
        assertTrue(ui.showsResult)
        val end = ui.dailyEnd!!
        assertTrue(end.passed)
        assertEquals(puzzle.best, end.best)
        assertEquals(puzzle.best, end.moves)
        assertEquals(end.moves, end.marks.size)
        assertEquals("Pinfold #${puzzle.number} ${puzzle.best}/${puzzle.best}", end.headline)
        assertFalse(Regex("[a-z][0-9]").containsMatchIn(end.share!!))
    }

    @Test
    fun `Retry starts a new attempt at the same puzzle`() = runTest(dispatcher) {
        val entries = Entries()
        var now = lateEvening
        val vm = game(journal = entries, daily = Days { day }, clock = { now })
        now += 5_000
        vm.playAgain()
        val retry = entries.started.single()
        assertEquals(puzzle, (retry.record as DailyRecord).puzzle)
        assertNotEquals(lateEvening, retry.startedAt)
    }

    @Test
    fun `View result opens the solved attempt again and counts it once`() = runTest(dispatcher) {
        val days = Days { day }
        val record = DailyRecord(puzzle, line)
        val end = record.replay()
        val solved = GameLogEntry.start(record, puzzle.strength.text, threatHighlight = false, now = lateEvening)
            .copy(record = record, outcome = Outcome.FINISHED, red = end.score(Player.RED), blue = end.score(Player.BLUE))
        days.progress = days.progress.finished(DailyAttempt(day, lateEvening, passed = true, line))
        val entries = Entries()
        val vm = game(journal = entries, daily = days, start = solved)
        advanceUntilIdle()
        assertTrue(vm.ui.value.showsResult)
        assertEquals(1, days.progress.attempts(day).size)
        assertTrue(entries.started.isEmpty() && entries.saved.isEmpty() && entries.finished.isEmpty())
    }
}

/** The day at a time in UTC, as the tests' clock gives it. */
private object DailyClockTestDays {
    fun of(millis: Long): Day = DailyClock.today(millis, java.util.TimeZone.getTimeZone("UTC"))
}
