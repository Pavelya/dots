package io.github.pavelya.dots.game

import dots.engine.GameState
import dots.engine.Player

/** The computer's faces on the canvas game frames (docs/design/canvas/draw.py, `face`). */
enum class Mood { HAPPY, SMUG, UPSET, DETERMINED, SULKY }

/** What the computer says in its speech bubble (the canvas and the wireframes, D-06 and D-07). */
enum class Bubble { GOOD_LUCK, YOUR_MOVE, HEY, OUCH, WELL_PLAYED, AGAIN }

/** Points [by] won with the last move: one capture or several closed by the same dot. */
data class LastCapture(val by: Player, val points: Int)

/**
 * How the computer above the board looks at a moment of the game: its [mood] and what it says,
 * if anything. The canvas frames show three moments (the level opens, a threat, a big capture)
 * and the wireframes add the rest.
 */
data class OpponentLook(val mood: Mood, val bubble: Bubble? = null) {
    companion object {
        /** From this many points at once, a capture is a big one and the face falls (D-06, "Big capture"). */
        const val BIG_CAPTURE = 5

        /**
         * The look for [state], against a [human] who plays the other colour. [lastCapture] is what
         * the last move won, if anything; [humanInDanger] is true when one more computer dot would
         * capture some of the human's dots.
         */
        fun of(state: GameState, human: Player, lastCapture: LastCapture?, humanInDanger: Boolean): OpponentLook = when {
            state.isOver -> when (state.winner) {
                human -> OpponentLook(Mood.SULKY, Bubble.WELL_PLAYED)
                null -> OpponentLook(Mood.DETERMINED)
                else -> OpponentLook(Mood.SMUG, Bubble.AGAIN)
            }
            lastCapture?.by == human && lastCapture.points >= BIG_CAPTURE -> OpponentLook(Mood.UPSET, Bubble.OUCH)
            lastCapture?.by == human -> OpponentLook(Mood.UPSET, Bubble.HEY)
            humanInDanger && state.toMove == human -> OpponentLook(Mood.SMUG, Bubble.YOUR_MOVE)
            state.dotsPlaced(human) == 0 -> OpponentLook(Mood.HAPPY, Bubble.GOOD_LUCK)
            lastCapture != null -> OpponentLook(Mood.SMUG)
            else -> OpponentLook(Mood.DETERMINED)
        }
    }
}
