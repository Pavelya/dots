package dots.daily

import dots.engine.Action
import dots.engine.GameState

/**
 * The text a player shares after a daily puzzle. It shows how the attempts went but not the
 * board or the moves, so it spoils nothing for those who have not played yet:
 *
 *     Pinfold #42 3/3
 *     ⚪🔵 ❌
 *     ⚪⚪🔴 ✅
 *
 * The first line has the puzzle's number, then the moves of the attempt that passed and the
 * [DailyPuzzle.best] (`X/3` if none passed). Then one row per attempt, one symbol for each of the
 * player's moves: 🔴 the player captured, 🔵 the computer captured in its reply, ⚪ neither. The
 * row ends with ✅ for a pass and ❌ for a failure; an attempt still going has no mark.
 */
object DailyShare {
    const val GAME = "Pinfold"
    const val CAPTURED_SYMBOL = "🔴"
    const val LOST_SYMBOL = "🔵"
    const val QUIET_SYMBOL = "⚪"
    const val PASSED = "✅"
    const val FAILED = "❌"

    /** What one of the player's moves led to, as the share text shows it. */
    enum class Mark(val symbol: String) {
        /** The player captured. */
        CAPTURED(CAPTURED_SYMBOL),

        /** The computer captured in its reply. */
        LOST(LOST_SYMBOL),

        /** Neither side captured. */
        QUIET(QUIET_SYMBOL),
    }

    /** [attempts] are the actions of each attempt from the puzzle's start, in the order they were played. */
    fun text(puzzle: DailyPuzzle, attempts: List<List<Action>>): String {
        val played = attempts.map { play(puzzle, it) }
        val passed = played.firstOrNull { it.passed == true }
        return buildString {
            append(headline(puzzle, passed?.marks?.size))
            for (attempt in played) {
                append('\n').append(attempt.marks.joinToString("") { it.symbol })
                when (attempt.passed) {
                    true -> append(" $PASSED")
                    false -> append(" $FAILED")
                    null -> Unit
                }
            }
        }
    }

    /** The share text's first line: the puzzle's number, then the [moves] of the pass and the best (`X/3` with no pass). */
    fun headline(puzzle: DailyPuzzle, moves: Int?): String = "$GAME #${puzzle.number} ${moves?.toString() ?: "X"}/${puzzle.best}"

    /** One mark for each of the player's moves in [actions], an attempt from the puzzle's start. */
    fun marks(puzzle: DailyPuzzle, actions: List<Action>): List<Mark> = play(puzzle, actions).marks

    /** One attempt: a mark per move of the player's, and whether it passed (null while it goes on). */
    private class Attempt(val marks: List<Mark>, val passed: Boolean?)

    private fun play(puzzle: DailyPuzzle, actions: List<Action>): Attempt {
        var state: GameState = puzzle.newGame()
        val marks = ArrayList<Mark>()
        for (action in actions) {
            require(!puzzle.isOver(state)) { "an action after the end of the puzzle: ${Action.word(action)}" }
            require(state.isLegal(action)) { "an illegal action: ${Action.word(action)}" }
            val mover = state.toMove
            val before = state
            state = state.apply(action).state
            if (mover == puzzle.human) {
                marks += Mark.QUIET
            }
            if (marks.isEmpty()) continue
            // A capture marks the player's last move: their own capture, or the computer's in reply.
            when {
                state.score(puzzle.human) > before.score(puzzle.human) -> marks[marks.lastIndex] = Mark.CAPTURED
                state.score(puzzle.computer) > before.score(puzzle.computer) && marks.last() != Mark.CAPTURED -> marks[marks.lastIndex] = Mark.LOST
            }
        }
        val passed = if (puzzle.isOver(state)) puzzle.isPassed(state) else null
        return Attempt(marks, passed)
    }
}
