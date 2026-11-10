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
    const val CAPTURED = "🔴"
    const val LOST = "🔵"
    const val QUIET = "⚪"
    const val PASSED = "✅"
    const val FAILED = "❌"

    /** [attempts] are the actions of each attempt from the puzzle's start, in the order they were played. */
    fun text(puzzle: DailyPuzzle, attempts: List<List<Action>>): String {
        val played = attempts.map { play(puzzle, it) }
        val passed = played.firstOrNull { it.passed == true }
        val score = (passed?.moves?.size?.toString() ?: "X") + "/" + puzzle.best
        return buildString {
            append("$GAME #${puzzle.number} $score")
            for (attempt in played) {
                append('\n').append(attempt.moves.joinToString(""))
                when (attempt.passed) {
                    true -> append(" $PASSED")
                    false -> append(" $FAILED")
                    null -> Unit
                }
            }
        }
    }

    /** One attempt: a symbol per move of the player's, and whether it passed (null while it goes on). */
    private class Attempt(val moves: List<String>, val passed: Boolean?)

    private fun play(puzzle: DailyPuzzle, actions: List<Action>): Attempt {
        var state: GameState = puzzle.newGame()
        val moves = ArrayList<String>()
        for (action in actions) {
            require(!puzzle.isOver(state)) { "an action after the end of the puzzle: ${Action.word(action)}" }
            require(state.isLegal(action)) { "an illegal action: ${Action.word(action)}" }
            val mover = state.toMove
            val before = state
            state = state.apply(action).state
            if (mover == puzzle.human) {
                moves += QUIET
            }
            if (moves.isEmpty()) continue
            // A capture marks the player's last move: their own capture, or the computer's in reply.
            when {
                state.score(puzzle.human) > before.score(puzzle.human) -> moves[moves.lastIndex] = CAPTURED
                state.score(puzzle.computer) > before.score(puzzle.computer) && moves.last() != CAPTURED -> moves[moves.lastIndex] = LOST
            }
        }
        val passed = if (puzzle.isOver(state)) puzzle.isPassed(state) else null
        return Attempt(moves, passed)
    }
}
