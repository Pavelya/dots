package io.github.pavelya.dots.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dots.daily.Daily
import dots.daily.DailyAttempt
import dots.daily.DailyClock
import dots.daily.DailyProgress
import dots.daily.DailyPuzzle
import dots.daily.DailyShare
import dots.daily.Day
import dots.daily.Streak
import dots.engine.Action
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.TimeZone

/**
 * The daily puzzle's clock: the phone's time and time zone, read each time they are asked for, so
 * a new day, a trip or a change of the clocks shows at once. [shiftDays] moves it forward, for the
 * developer menu's "a year ahead" (the puzzles start on 1 January 2027).
 */
class PuzzleClock(
    private val millis: () -> Long = System::currentTimeMillis,
    private val zone: () -> TimeZone = TimeZone::getDefault,
) {
    @Volatile
    var shiftDays: Int = 0

    /** The time now, in milliseconds since 1970, moved by [shiftDays]. */
    fun now(): Long = millis() + shiftDays * DailyClock.DAY_MILLIS

    /** Today's date, whose puzzle Today's puzzle shows. */
    fun today(): Day = DailyClock.today(now(), zone())

    /** Milliseconds to the next puzzle, at the next midnight. */
    fun toNextPuzzle(): Long = DailyClock.toNextPuzzle(now(), zone())

    companion object {
        /** The developer menu's "a year ahead": 2026 has no puzzles, 2027 has. */
        const val YEAR_AHEAD_DAYS = 365
    }
}

/**
 * How a daily attempt ended, once it is saved: whether it [passed], the player's [moves] against
 * the [best] possible, the [streak] now, a [mark] per move (what the share text shows), and the
 * [share] text, which has no spoilers. A failed attempt has no share text.
 */
data class DailyEnd(
    val passed: Boolean,
    val moves: Int,
    val best: Int,
    val streak: Streak,
    val marks: List<DailyShare.Mark>,
    val share: String?,
) {
    /** The share text's first line, as the "what you share" box on Result shows it. */
    val headline: String? get() = share?.lineSequence()?.first()

    companion object {
        /** The end of [attempt] at [puzzle], with the daily [progress] that already counts it, on [today]. */
        fun of(puzzle: DailyPuzzle, attempt: DailyAttempt, progress: DailyProgress, today: Day): DailyEnd {
            val marks = DailyShare.marks(puzzle, attempt.actions)
            return DailyEnd(
                passed = attempt.passed,
                moves = marks.size,
                best = puzzle.best,
                streak = progress.streak(today),
                marks = marks,
                share = if (attempt.passed) progress.shareText(puzzle) else null,
            )
        }
    }
}

/** Where a daily attempt's end is saved ([DailyRepository]). */
fun interface DailyJournal {
    /**
     * Saves the attempt at [puzzle] that started at [startedAt] and ended after [actions], and
     * returns what Result shows. The same attempt reported again counts once.
     */
    suspend fun finished(puzzle: DailyPuzzle, startedAt: Long, actions: List<Action>, passed: Boolean): DailyEnd
}

/**
 * The days solved and the recent attempts ([DailyProgress]), kept in the progress store, so
 * Android's backup takes the streak with the stars. The day a puzzle belongs to is its own, not
 * the day the attempt ended: an attempt that runs past midnight counts for the day it began.
 */
class DailyRepository(
    private val store: DataStore<Preferences>,
    scope: CoroutineScope,
    private val clock: () -> PuzzleClock,
) : DailyJournal {

    val progress: Flow<DailyProgress> = store.data.map(::read)

    /** The daily progress as last read; null until it is first read. */
    val saved: StateFlow<DailyProgress?> = progress.stateIn(scope, SharingStarted.Eagerly, null)

    /** Today's puzzle; null before the first puzzle or after the last one the app has. */
    fun today(): DailyPuzzle? = Daily.puzzle(clock().today())

    override suspend fun finished(puzzle: DailyPuzzle, startedAt: Long, actions: List<Action>, passed: Boolean): DailyEnd {
        val attempt = DailyAttempt(puzzle.day, startedAt, passed, actions)
        var after = DailyProgress.NONE
        store.edit { prefs ->
            after = read(prefs).finished(attempt)
            prefs[SOLVED] = after.solvedText
            prefs[ATTEMPTS] = after.attemptsText
        }
        return DailyEnd.of(puzzle, attempt, after, clock().today())
    }

    /** Clears the daily progress; for the developer menu. */
    suspend fun clear() {
        store.edit {
            it.remove(SOLVED)
            it.remove(ATTEMPTS)
        }
    }

    private fun read(prefs: Preferences): DailyProgress = DailyProgress.parse(prefs[SOLVED].orEmpty(), prefs[ATTEMPTS].orEmpty())

    private companion object {
        /** The days solved: `2027-01-04 2027-01-05`. */
        val SOLVED = stringPreferencesKey("daily_solved")

        /** The recent finished attempts, one a line ([DailyAttempt]). */
        val ATTEMPTS = stringPreferencesKey("daily_attempts")
    }
}
