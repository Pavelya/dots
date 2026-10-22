package dots.ai

import dots.engine.GameState
import dots.engine.Pos

/** A computer opponent. */
interface Ai {
    /** The move for [GameState.toMove], or null to pass (also returned when the game is over). */
    fun chooseMove(state: GameState): Pos?
}

/**
 * How hard a [SearchAi] plays.
 *
 * @param depth plies searched ahead: 1 only grabs captures, 2 also avoids giving one away,
 *   3 can set up a double threat, 4 also sees the answer to it.
 * @param breadth moves kept per ply after ordering; the root keeps twice as many.
 * @param mistakeRate chance per turn of playing a random nearby move instead of the best one.
 * @param reach only cells within this many steps of an existing dot are considered.
 */
data class AiConfig(
    val depth: Int,
    val breadth: Int,
    val mistakeRate: Double = 0.0,
    val reach: Int = 2,
) {
    init {
        require(depth >= 1) { "depth must be at least 1" }
        require(breadth >= 1) { "breadth must be at least 1" }
        require(mistakeRate in 0.0..1.0) { "mistakeRate must be a probability" }
        require(reach >= 1) { "reach must be at least 1" }
    }

    companion object {
        val EASY = AiConfig(depth = 1, breadth = 12, mistakeRate = 0.35)
        val NORMAL = AiConfig(depth = 2, breadth = 10, mistakeRate = 0.08)
        val HARD = AiConfig(depth = 3, breadth = 8)
        val EXPERT = AiConfig(depth = 4, breadth = 6)
    }
}
