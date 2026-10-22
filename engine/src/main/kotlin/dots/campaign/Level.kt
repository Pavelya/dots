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

    /** Dots the computer starts with. */
    val headStart: Int get() = map.count { it == Player.BLUE.symbol }

    fun newGame(): GameState = GameState.fromAscii(map, dotsPerPlayer, first = human)
}
