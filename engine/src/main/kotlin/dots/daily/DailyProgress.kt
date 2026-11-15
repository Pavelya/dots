package dots.daily

import dots.engine.Action

/**
 * One finished attempt at the puzzle of [day]: when it [startedAt] (milliseconds since 1970, which
 * tells attempts apart), whether it [passed], and its [actions] from the puzzle's start. Its text
 * form is one line:
 *
 *     2027-01-05 1798000000000 pass e5 f6 d4 c3
 */
data class DailyAttempt(val day: Day, val startedAt: Long, val passed: Boolean, val actions: List<Action>) {
    fun toText(): String = buildString {
        append("$day $startedAt ${if (passed) PASS else FAIL}")
        for (action in actions) append(' ').append(Action.word(action))
    }

    companion object {
        private const val PASS = "pass"
        private const val FAIL = "fail"

        /** Reads a line written by [toText]; null if it is not one. */
        fun parse(line: String): DailyAttempt? {
            val words = line.trim().split(Regex("\\s+"))
            if (words.size < 3) return null
            val day = Day.parse(words[0]) ?: return null
            val startedAt = words[1].toLongOrNull() ?: return null
            val passed = when (words[2]) {
                PASS -> true
                FAIL -> false
                else -> return null
            }
            val actions = try {
                words.drop(3).map(Action::parse)
            } catch (_: IllegalArgumentException) {
                return null
            }
            return DailyAttempt(day, startedAt, passed, actions)
        }
    }
}

/**
 * What the player has done in the daily puzzles: every day whose puzzle they [solved], and the
 * finished [attempts] of the last [KEEP_DAYS] days that have any, for Result and the share text.
 * The app stores it as two lines of text ([solvedText], [attemptsText]); what cannot be read is
 * left out.
 */
class DailyProgress(solved: Set<Day> = emptySet(), attempts: List<DailyAttempt> = emptyList()) {
    val solved: Set<Day> = solved.toSortedSet()
    val attempts: List<DailyAttempt> = attempts.sortedWith(compareBy({ it.day }, { it.startedAt }))

    /** The streak as it stands on [today]. */
    fun streak(today: Day): Streak = Streak.of(solved, today)

    fun isSolved(day: Day): Boolean = day in solved

    /** The finished attempts at [day]'s puzzle, in the order they were played. */
    fun attempts(day: Day): List<DailyAttempt> = attempts.filter { it.day == day }

    /** The first attempt that solved [day]'s puzzle, or null. */
    fun solution(day: Day): DailyAttempt? = attempts.firstOrNull { it.day == day && it.passed }

    /**
     * With [attempt] added. The same attempt again (same day and start) changes nothing, so a
     * finished game that is opened again, or reported twice, counts once. Only the attempts of the
     * last [KEEP_DAYS] days with any are kept.
     */
    fun finished(attempt: DailyAttempt): DailyProgress {
        if (attempts.any { it.day == attempt.day && it.startedAt == attempt.startedAt }) return this
        val all = attempts + attempt
        val kept = all.map { it.day }.distinct().sorted().takeLast(KEEP_DAYS).toSet()
        return DailyProgress(if (attempt.passed) solved + attempt.day else solved, all.filter { it.day in kept })
    }

    /** The share text of [puzzle]'s day: its attempts, up to and including the one that solved it. */
    fun shareText(puzzle: DailyPuzzle): String {
        val played = attempts(puzzle.day)
        val upToPass = played.indexOfFirst { it.passed }.let { if (it < 0) played else played.subList(0, it + 1) }
        return DailyShare.text(puzzle, upToPass.map { it.actions })
    }

    /** The solved days, one line: `2027-01-04 2027-01-05`. */
    val solvedText: String get() = solved.joinToString(" ")

    /** The attempts, one line each ([DailyAttempt.toText]). */
    val attemptsText: String get() = attempts.joinToString("\n") { it.toText() }

    override fun equals(other: Any?): Boolean = other is DailyProgress && solved == other.solved && attempts == other.attempts

    override fun hashCode(): Int = 31 * solved.hashCode() + attempts.hashCode()

    override fun toString(): String = "DailyProgress(solved=$solvedText, attempts=${attempts.size})"

    companion object {
        /** Attempts are kept for this many days: the days with any, the latest ones. */
        const val KEEP_DAYS = 7

        val NONE = DailyProgress()

        fun parse(solvedText: String, attemptsText: String): DailyProgress = DailyProgress(
            solvedText.split(' ').mapNotNull { Day.parse(it.trim()) }.toSet(),
            attemptsText.lines().filter { it.isNotBlank() }.mapNotNull(DailyAttempt::parse),
        )
    }
}
