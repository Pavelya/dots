package dots.ai

import dots.engine.GameState
import dots.engine.Player

object Match {
    /** Plays [start] to the end with a computer on each side. */
    fun playOut(start: GameState, red: Ai, blue: Ai): GameState {
        var state = start
        while (!state.isOver) {
            val ai = if (state.toMove == Player.RED) red else blue
            val move = ai.chooseMove(state)
            state = if (move == null) state.pass() else state.play(move).state
        }
        return state
    }
}
