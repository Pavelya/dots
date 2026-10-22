package dots.campaign

import dots.ai.AiConfig
import dots.engine.GameState
import dots.engine.Player

/**
 * A single-player level: the human (red, moving first) plays the computer (blue).
 *
 * Difficulty comes from three knobs: the computer's [ai] strength, the [map] it starts with
 * (`B` dots are its head start, `R` dots are the human's), and the [dotsPerPlayer] budget.
 */
data class Level(
    val id: Int,
    val name: String,
    val map: String,
    val dotsPerPlayer: Int,
    val ai: AiConfig,
) {
    val human: Player get() = Player.RED
    val computer: Player get() = Player.BLUE

    /** How many more dots than the human the computer starts with. */
    val headStart: Int get() = map.count { it == computer.symbol } - map.count { it == human.symbol }

    fun newGame(): GameState = GameState.fromAscii(map, dotsPerPlayer, first = human)

    /** A level is passed only by winning: a draw (for instance 0-0) is not enough. */
    fun isPassed(end: GameState): Boolean = end.isOver && end.winner == human
}
