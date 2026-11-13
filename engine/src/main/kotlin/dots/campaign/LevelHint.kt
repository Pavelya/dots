package dots.campaign

import dots.ai.AiConfig
import dots.ai.SearchAi
import dots.engine.Action
import dots.engine.Booster
import dots.engine.GameState
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The [Booster.HINT] in a level ([Level.hint]): the move that serves the player's mission. Two
 * steps share the hint's 1.5 seconds.
 *
 * 1. **The hard computer, playing for the player.** [AiConfig.HINT], thinking for at most
 *    [SEARCH_MILLIS], with the player's side of the mission as its goal ([Level.playerGoal]). It
 *    weighs captures as the mission does, and sees a move that passes the mission, or one after
 *    which the computer could fail it, as far ahead as it searches.
 * 2. **The puzzle solver, in a puzzle mission** ([Solver.isPuzzle]): [Solver.lookAhead] over the
 *    mission's moves left, at most [Solver.MAX_MOVES]. It tries every reply of the computer's, so
 *    where it finishes it is exact. It swaps the first step's move for the quickest move that
 *    passes the mission whatever the computer does, if it finds one. If not, it keeps that move
 *    unless the computer could then force the mission to fail (or, in "capture" and "target", put
 *    it out of reach), and swaps it for one that avoids that if there is one. It stops after
 *    [SOLVER_MILLIS], or [SOLVER_POSITIONS] positions per search, and the first step's move
 *    stands.
 *
 * The solver is needed: at the start of the campaign's puzzle levels the first step alone misses
 * a passing first move in about one level in six, and the solver finds one in every level.
 * Whole-game missions ("win", "win by N", "win with fewer dots") get the first step's move.
 */
internal object LevelHint {
    /** How long the first step may think. */
    const val SEARCH_MILLIS = 1_000L

    /** How long the second step may think. */
    const val SOLVER_MILLIS = 400L

    /** How many positions each search of the second step may look at. */
    const val SOLVER_POSITIONS = 50_000L

    /** The hint for the player in [state]; [solverTime] is for tests that must not depend on the machine's speed. */
    fun find(level: Level, state: GameState, random: Random, solverTime: Duration = SOLVER_MILLIS.milliseconds): Action.Hint? {
        if (!state.canUse(Booster.HINT)) return null
        val config = AiConfig.HINT.copy(timeLimitMillis = SEARCH_MILLIS)
        val preferred = Action.Place(SearchAi(config, random, level.playerGoal).chooseMove(state) ?: return null)
        val moves = movesLeft(level, state)
        val move = if (moves < 1) preferred else Solver(timeLimit = solverTime).lookAhead(level, state, moves, preferred, SOLVER_POSITIONS)
        return Action.Hint((move as? Action.Place ?: preferred).pos).takeIf { state.isLegal(it) }
    }

    /** How many moves of the player's the mission has left in [state], at most [Solver.MAX_MOVES]. */
    private fun movesLeft(level: Level, state: GameState): Int {
        val left = when (val mission = level.mission) {
            is Mission.Capture -> mission.moves - state.dotsPlaced(level.human)
            // The player moves before each of the computer's dots.
            is Mission.Survive -> mission.moves - state.dotsPlaced(level.computer)
            else -> Solver.MAX_MOVES
        }
        return minOf(left, Solver.MAX_MOVES, state.dotsLeft(level.human) ?: Solver.MAX_MOVES)
    }
}
