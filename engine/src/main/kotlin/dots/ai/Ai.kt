package dots.ai

import dots.engine.Action
import dots.engine.GameState

/** A computer opponent. */
interface Ai {
    /** What [GameState.toMove] should do now. Only call this while the game is not over. */
    fun choose(state: GameState): Action

    /**
     * As [choose], but may give up early once [isCancelled] returns true; the action returned
     * then is legal but should be thrown away. By default it simply ignores [isCancelled].
     */
    fun choose(state: GameState, isCancelled: () -> Boolean): Action = choose(state)
}

/**
 * How hard a [SearchAi] plays.
 *
 * @param depth plies searched ahead: 1 only grabs captures, 2 also avoids giving one away,
 *   3 can set up a double threat, 4 also sees the answer to it.
 * @param breadth moves kept per ply after ordering; the root keeps twice as many.
 * @param mistakeRate chance per turn of playing a random nearby move instead of the best one.
 * @param variety so that a position does not always get the same answer, each candidate move gets
 *   a random bonus of 0 to this many points before the best is picked. A move this far below the
 *   best is never played, a move just below it often is. For scale, a captured dot is worth 10,000
 *   and touching one more dot 2 or 3; 0 picks only between moves with exactly the best score.
 * @param reach only cells within this many steps of an existing dot are considered.
 * @param attackReach the computer looks for enemy groups it could capture with at most this many
 *   more dots and tries those cells first; 0 turns it off.
 * @param dangerReach the same for its own groups the opponent could capture: it considers
 *   blocking those cells first.
 * @param timeLimitMillis if set, the computer answers within about this many milliseconds,
 *   searching as deep as time allows up to [depth]; null searches to [depth] however long it takes.
 */
data class AiConfig(
    val depth: Int,
    val breadth: Int,
    val mistakeRate: Double = 0.0,
    val variety: Int = 0,
    val reach: Int = 2,
    val attackReach: Int = 0,
    val dangerReach: Int = 0,
    val timeLimitMillis: Long? = null,
) {
    init {
        require(depth >= 1) { "depth must be at least 1" }
        require(breadth >= 1) { "breadth must be at least 1" }
        require(mistakeRate in 0.0..1.0) { "mistakeRate must be a probability" }
        require(variety >= 0) { "variety must not be negative" }
        require(reach >= 1) { "reach must be at least 1" }
        require(attackReach >= 0 && dangerReach >= 0) { "threat reach must not be negative" }
        require(timeLimitMillis == null || timeLimitMillis > 0) { "timeLimitMillis must be positive" }
    }

    companion object {
        val EASY = AiConfig(depth = 1, breadth = 12, mistakeRate = 0.35)
        val NORMAL = AiConfig(depth = 2, breadth = 10, mistakeRate = 0.08, variety = 10, attackReach = 2, dangerReach = 1)
        val HARD = AiConfig(depth = 3, breadth = 8, variety = 5, attackReach = 3, dangerReach = 2)
        /** Looks one move further than [HARD], at about three times the cost. Benchmarks show no gain yet. */
        val EXPERT = AiConfig(depth = 4, breadth = 8, variety = 5, attackReach = 3, dangerReach = 3)
    }
}
