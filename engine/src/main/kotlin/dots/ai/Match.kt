package dots.ai

import dots.engine.GameState
import dots.engine.Player

object Match {
    /** Plays [start] with a computer on each side until [isOver] (by default, the end of the game). */
    fun playOut(start: GameState, red: Ai, blue: Ai, isOver: (GameState) -> Boolean = { it.isOver }): GameState {
        var state = start
        while (!isOver(state)) {
            val ai = if (state.toMove == Player.RED) red else blue
            state = state.apply(ai.choose(state)).state
        }
        return state
    }
}
